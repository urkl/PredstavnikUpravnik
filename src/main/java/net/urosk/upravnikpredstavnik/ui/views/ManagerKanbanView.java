package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import net.urosk.upravnikpredstavnik.config.AppProcessProperties;
import net.urosk.upravnikpredstavnik.data.entity.*;
import net.urosk.upravnikpredstavnik.data.repository.BuildingRepository;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;
import net.urosk.upravnikpredstavnik.service.AuditService;
import net.urosk.upravnikpredstavnik.service.PdfExportService;
import net.urosk.upravnikpredstavnik.ui.components.CaseFormDialog;
import net.urosk.upravnikpredstavnik.ui.components.CommentsDialog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "kanban", layout = MainLayout.class)
@PageTitle("Kanban Pregled")
@PermitAll
public class ManagerKanbanView extends VerticalLayout {

    private final CaseRepository caseRepository;
    private final BuildingRepository buildingRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final PdfExportService pdfExportService;
    private final AppProcessProperties processProperties;
    private final AuthenticatedUser authenticatedUser;

    private final Map<String, VerticalLayout> statusColumns = new HashMap<>();
    private final TextField searchField = new TextField();
    private final HorizontalLayout board = new HorizontalLayout();

    public ManagerKanbanView(CaseRepository caseRepository, AppProcessProperties processProperties, AuthenticatedUser authenticatedUser, BuildingRepository buildingRepository, UserRepository userRepository, AuditService auditService, PdfExportService pdfExportService) {
        this.caseRepository = caseRepository;
        this.processProperties = processProperties;
        this.authenticatedUser = authenticatedUser;
        this.buildingRepository = buildingRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.pdfExportService = pdfExportService;

        setSizeFull();
        setSpacing(false);
        setPadding(false);
        addClassName("kanban-view");

        add(createToolbar());

        board.setSizeFull();
        board.setSpacing(true);
        board.addClassName("kanban-board");
        for (String statusKey : processProperties.getStatuses().keySet()) {
            VerticalLayout column = createStatusColumn(statusKey, processProperties.getStatuses().get(statusKey));
            statusColumns.put(statusKey, column);
            board.add(column);
        }

        add(board);
        loadAndDisplayCases("");
    }

    private HorizontalLayout createToolbar() {
        searchField.setPlaceholder("Išči...");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> loadAndDisplayCases(e.getValue()));

        Button addCaseButton = new Button("Nova zadeva", new Icon(VaadinIcon.PLUS));
        addCaseButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addCaseButton.addClickListener(e -> {
            CaseFormDialog dialog = new CaseFormDialog(new Case(), caseRepository, buildingRepository, userRepository, processProperties, authenticatedUser, auditService, pdfExportService, () -> loadAndDisplayCases(searchField.getValue()));
            dialog.open();
        });

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addCaseButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.setPadding(true);
        toolbar.setBoxSizing(BoxSizing.BORDER_BOX);
        return toolbar;
    }

    private void loadAndDisplayCases(String searchTerm) {
        statusColumns.values().forEach(column -> column.getChildren().filter(Card.class::isInstance).forEach(column::remove));
        List<Case> cases = caseRepository.findAll();
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String lowerCaseFilter = searchTerm.toLowerCase().trim();
            cases = cases.stream().filter(c ->
                    (c.getTitle() != null && c.getTitle().toLowerCase().contains(lowerCaseFilter)) ||
                            (c.getDescription() != null && c.getDescription().toLowerCase().contains(lowerCaseFilter))
            ).collect(Collectors.toList());
        }
        cases.forEach(caseItem -> {
            VerticalLayout column = statusColumns.get(caseItem.getStatus());
            if (column != null) {
                column.add(createCaseCard(caseItem));
            }
        });
    }

// ... v razredu ManagerKanbanView ...

    private VerticalLayout createStatusColumn(String statusKey, String displayName) {
        H3 title = new H3(displayName);
        title.addClassNames("title");
        VerticalLayout column = new VerticalLayout(title);
        column.setClassName("kanban-column");

        DropTarget<VerticalLayout> dropTarget = DropTarget.create(column);
        dropTarget.setActive(true);

        dropTarget.addDropListener(event -> {
            event.getDragSourceComponent().ifPresent(draggedComponent -> {
                // Preverimo, ali je premaknjen element res kartica
                if (draggedComponent instanceof Card) {
                    event.getDragData()
                            .flatMap(data -> caseRepository.findById((String) data))
                            .ifPresent(caseToUpdate -> {
                                // Preverimo, ali je stolpec drugačen od trenutnega
                                if (!column.equals(draggedComponent.getParent().orElse(null))) {

                                    // --- LOGIKA ZA POSODOBITEV IN OSVEŽITEV ---
                                    String oldStatus = caseToUpdate.getStatus();
                                    caseToUpdate.setStatus(statusKey);
                                    caseRepository.save(caseToUpdate);

                                    String userEmail = authenticatedUser.get().map(User::getEmail).orElse("SYSTEM");
                                    String details = "Status spremenjen iz '" + oldStatus + "' v '" + statusKey + "'";
                                    auditService.log("SPREMEMBA STATUSA", Case.class, caseToUpdate.getId(), details, userEmail);

                                    // === KLJUČNI POPRAVEK: Osveži celoten pogled ===
                                    // Namesto ročnega premikanja komponente, ponovno naložimo vse zadeve.
                                    // S tem zagotovimo, da so vsi podatki na vseh karticah sveži.
                                    loadAndDisplayCases(searchField.getValue());
                                    // ===============================================
                                }
                            });
                }
            });
        });

        return column;
    }

// ... ostale metode ostanejo enake ...

    private Card createCaseCard(Case caseItem) {
        Card card = new Card();
        card.setWidth("320px");
        card.setClassName("kanban-card");
        DragSource.create(card).setDragData(caseItem.getId());

        // GLAVA KARTICE
        Span title = new Span(caseItem.getTitle());
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        HorizontalLayout titleRow = new HorizontalLayout(title);
        if (caseItem.getPriority() != null) {
            Icon priorityIcon = new Icon(VaadinIcon.FLAG);
            priorityIcon.setColor(caseItem.getPriority().getColor());
            priorityIcon.setTooltipText("Prioriteta: " + caseItem.getPriority().getDisplayName());
            titleRow.add(priorityIcon);
        }
        Button editButton = new Button(new Icon(VaadinIcon.PENCIL), e -> new CaseFormDialog(caseItem, caseRepository, buildingRepository, userRepository, processProperties, authenticatedUser, auditService, pdfExportService, () -> loadAndDisplayCases(searchField.getValue())).open());
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        editButton.setTooltipText("Uredi zadevo");
        HorizontalLayout header = new HorizontalLayout(titleRow, editButton);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setWidthFull();

        // VSEBINA KARTICE
        Span description = new Span(caseItem.getDescription());
        description.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Vertical.SMALL);

        VerticalLayout detailsLayout = new VerticalLayout(description);
        detailsLayout.setPadding(false);
        detailsLayout.setSpacing(false);

        // === POPRAVLJENO: Prikaz objektov z razmikom ===
        if (caseItem.getBuildings() != null && !caseItem.getBuildings().isEmpty()) {
            Icon buildingIcon = VaadinIcon.HOME_O.create();
            Span buildingText = new Span(caseItem.getBuildings().stream().map(Building::getName).collect(Collectors.joining(", ")));
            HorizontalLayout buildingsLayout = new HorizontalLayout(buildingIcon, buildingText);
            buildingsLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            buildingsLayout.setSpacing(true);
            buildingsLayout.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.XSMALL);
            detailsLayout.add(buildingsLayout);
        }

        // === POPRAVLJENO: Prikaz koordinatorjev z razmikom ===
        if (caseItem.getCoordinators() != null && !caseItem.getCoordinators().isEmpty()) {
            Icon coordinatorIcon = VaadinIcon.USER_STAR.create();
            Span coordinatorText = new Span("Koordinatorji: " + caseItem.getCoordinators().stream().map(User::getName).collect(Collectors.joining(", ")));
            HorizontalLayout coordinatorsLayout = new HorizontalLayout(coordinatorIcon, coordinatorText);
            coordinatorsLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            coordinatorsLayout.setSpacing(true);
            coordinatorsLayout.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.XSMALL);
            detailsLayout.add(coordinatorsLayout);
        }

        // PODNALOGE
        VerticalLayout subtasksContainer = createSubtasksComponent(caseItem);

        // NOGA KARTICE
        Span author = new Span("Prijavil: " + (caseItem.getAuthor() != null ? caseItem.getAuthor().getName() : "Neznan"));
        author.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.TERTIARY);
        Button commentsButton = new Button(new Icon(VaadinIcon.COMMENTS_O));
        commentsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        int commentCount = caseItem.getComments() != null ? caseItem.getComments().size() : 0;
        commentsButton.setText(String.valueOf(commentCount));
        commentsButton.setTooltipText("Prikaži komentarje (" + commentCount + ")");
        commentsButton.addClickListener(e -> new CommentsDialog(caseItem, caseRepository, authenticatedUser, auditService, () -> loadAndDisplayCases(searchField.getValue())).open());
        Div spacer = new Div();
        HorizontalLayout footer = new HorizontalLayout(author, spacer, commentsButton);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        footer.setWidthFull();
        footer.expand(spacer);

        card.add(header, detailsLayout, subtasksContainer, footer);
        addStatusTheme(card, caseItem.getStatus());
        return card;
    }

    private VerticalLayout createSubtasksComponent(Case caseItem) {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(false);
        container.addClassName(LumoUtility.Margin.Top.MEDIUM);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setWidthFull();
        updateProgressBar(progressBar, caseItem);
        if (caseItem.getSubtasks() != null && !caseItem.getSubtasks().isEmpty()) {
            container.add(progressBar);
        }

        VerticalLayout checkboxLayout = new VerticalLayout();
        checkboxLayout.setPadding(false);
        checkboxLayout.setSpacing(false);
        if (caseItem.getSubtasks() != null) {
            caseItem.getSubtasks().forEach(subtask -> checkboxLayout.add(createSubtaskCheckbox(subtask, caseItem, progressBar)));
        }
        container.add(checkboxLayout, createAddSubtaskControls(caseItem));
        return container;
    }

    private Checkbox createSubtaskCheckbox(Subtask subtask, Case parentCase, ProgressBar progressBar) {
        Checkbox checkbox = new Checkbox(subtask.getTask(), subtask.isCompleted());
        checkbox.addValueChangeListener(event -> {
            subtask.setCompleted(event.getValue());
            caseRepository.save(parentCase);
            updateProgressBar(progressBar, parentCase);
            String details = "Stanje podnaloge '" + subtask.getTask() + "' spremenjeno na " + (event.getValue() ? "dokončano." : "nedokončano.");
            String userEmail = authenticatedUser.get().map(User::getEmail).orElse("Neznan uporabnik");
            auditService.log("POSODOBITEV PODNALOGE", Case.class, parentCase.getId(), details,userEmail);
        });
        return checkbox;
    }

    private HorizontalLayout createAddSubtaskControls(Case caseItem) {
        TextField newSubtaskField = new TextField();
        newSubtaskField.setPlaceholder("Nova podnaloga...");
        newSubtaskField.setWidth("180px");

        Button confirmAddBtn = new Button(new Icon(VaadinIcon.PLUS), e -> {
            String taskText = newSubtaskField.getValue();
            if (taskText != null && !taskText.trim().isEmpty()) {
                if (caseItem.getSubtasks() == null) caseItem.setSubtasks(new ArrayList<>());
                caseItem.getSubtasks().add(new Subtask(taskText, false));
                Case savedCase = caseRepository.save(caseItem);
                String userEmail = authenticatedUser.get().map(User::getEmail).orElse("Neznan uporabnik");
                auditService.log("DODAJANJE PODNALOGE", Case.class, savedCase.getId(), "Dodana: " + taskText,userEmail);
                loadAndDisplayCases(searchField.getValue());
            }
        });
        confirmAddBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        HorizontalLayout layout = new HorizontalLayout(newSubtaskField, confirmAddBtn);
        layout.setAlignItems(FlexComponent.Alignment.BASELINE);
        return layout;
    }

    private void updateProgressBar(ProgressBar progressBar, Case caseItem) {
        if (caseItem.getSubtasks() == null || caseItem.getSubtasks().isEmpty()) {
            progressBar.setVisible(false);
        } else {
            progressBar.setVisible(true);
            long total = caseItem.getSubtasks().size();
            long completed = caseItem.getSubtasks().stream().filter(Subtask::isCompleted).count();
            progressBar.setValue((double) completed / total);
        }
    }

    private void addStatusTheme(Card card, String status) {
        card.removeClassName("status-predlog");
        card.removeClassName("status-v-delu");
        card.removeClassName("status-zakljuceno");
        String theme = switch (status) {
            case "PREDLOG", "V_PREGLEDU" -> "status-predlog";
            case "POTRJENO", "V_DELU" -> "status-v-delu";
            case "ZAKLJUCENO" -> "status-zakljuceno";
            default -> "";
        };
        card.addClassName(theme);
    }
}