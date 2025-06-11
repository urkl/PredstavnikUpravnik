package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import net.urosk.upravnikpredstavnik.config.AppProcessProperties;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.entity.Subtask;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Route(value = "kanban", layout = MainLayout.class)
@PageTitle("Kanban Pregled")
@PermitAll
public class ManagerKanbanView extends HorizontalLayout {

    private final CaseRepository caseRepository;
    private final Map<String, VerticalLayout> statusColumns = new HashMap<>();
    private final AppProcessProperties processProperties;

    public ManagerKanbanView(CaseRepository caseRepository, AppProcessProperties processProperties) {
        this.caseRepository = caseRepository;
        this.processProperties = processProperties;
        setSizeFull();
        setSpacing(true);
        addClassName("kanban-board");

        for (String statusKey : processProperties.getStatuses().keySet()) {
            String displayName = processProperties.getStatuses().get(statusKey);
            VerticalLayout column = createStatusColumn(statusKey, displayName);
            statusColumns.put(statusKey, column);
            add(column);
        }

        loadAndDisplayCases();
    }

    private VerticalLayout createStatusColumn(String statusKey, String displayName) {
        H3 title = new H3(displayName);
        VerticalLayout column = new VerticalLayout(title);
        column.addClassName("kanban-column");

        DropTarget<VerticalLayout> dropTarget = DropTarget.create(column);
        dropTarget.setActive(true);

        dropTarget.addDropListener(event -> {
            if (event.getDragData().isEmpty()) return;
            Optional<Component> dragSourceOpt = event.getDragSourceComponent();
            if (dragSourceOpt.isEmpty()) return;
            if (!(dragSourceOpt.get() instanceof Card draggedCard)) return;

            event.getDragData().flatMap(data -> caseRepository.findById((String) data)).ifPresent(caseToUpdate -> {
                if (!column.equals(draggedCard.getParent().orElse(null))) {
                    caseToUpdate.setStatus(statusKey);
                    caseRepository.save(caseToUpdate);
                    draggedCard.getParent().ifPresent(parent -> ((VerticalLayout) parent).remove(draggedCard));
                    column.add(draggedCard);
                    addStatusTheme(draggedCard, statusKey);
                }
            });
        });

        return column;
    }

    private void loadAndDisplayCases() {
        // Počistimo stolpce pred ponovnim risanjem
        statusColumns.values().forEach(column -> {
            column.getChildren()
                    .filter(component -> component instanceof Card)
                    .toList() // Uporabimo toList(), da se izognemo ConcurrentModificationException
                    .forEach(column::remove);
        });

        caseRepository.findAll().forEach(caseItem -> {
            VerticalLayout column = statusColumns.get(caseItem.getStatus());
            if (column != null) {
                Card card = createCaseCard(caseItem);
                DragSource<Card> dragSource = DragSource.create(card);
                dragSource.setEffectAllowed(EffectAllowed.ALL);
                dragSource.addDragStartListener(event -> dragSource.setDragData(caseItem.getId()));
                column.add(card);
            }
        });
    }


    /**
     * Ustvari interaktivno kartico za posamezno zadevo.
     */
    private Card createCaseCard(Case caseItem) {
        Card card = new Card();
        card.setWidth("300px");
        card.addClassName("kanban-card");

        // --- GLAVA KARTICE ---
        Span title = new Span(caseItem.getTitle());
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);

        Button editButton = new Button(new Icon(VaadinIcon.EXTERNAL_LINK));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        editButton.getElement().getStyle().set("margin-left", "auto");
        editButton.setTooltipText("Uredi zadevo");
        editButton.addClickListener(e -> UI.getCurrent().navigate(CaseDetailView.class, caseItem.getId()));

        HorizontalLayout cardHeader = new HorizontalLayout(title, editButton);
        cardHeader.setWidthFull();
        cardHeader.setAlignItems(Alignment.CENTER);
        card.add(cardHeader);

        // --- VSEBINA KARTICE ---
        Span description = new Span(caseItem.getDescription());
        description.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.SMALL);
        card.add(description);

        // --- PODNALOGE (SUBTASKS) ---
        VerticalLayout subtasksContainer = new VerticalLayout();
        subtasksContainer.setPadding(false);
        subtasksContainer.setSpacing(false);
        subtasksContainer.addClassName(LumoUtility.Margin.Top.MEDIUM);

        ProgressBar progressBar = createSubtaskProgressBar(caseItem);
        subtasksContainer.add(progressBar);

        VerticalLayout checkboxLayout = new VerticalLayout(); // Nov layout samo za checkboxe
        checkboxLayout.setPadding(false);
        checkboxLayout.setSpacing(false);

        if (caseItem.getSubtasks() != null) {
            caseItem.getSubtasks().forEach(subtask ->
                    checkboxLayout.add(createSubtaskCheckbox(subtask, caseItem, progressBar))
            );
        }
        subtasksContainer.add(checkboxLayout);

        HorizontalLayout addSubtaskControls = createAddSubtaskControls(caseItem, checkboxLayout, progressBar);
        subtasksContainer.add(addSubtaskControls);
        card.add(subtasksContainer);

        // --- NOGA KARTICE ---
        Span author = new Span("Prijavil: " + caseItem.getAuthor().getName());
        author.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.TERTIARY);

        Span timeInfo = new Span(formatTimeAgo(caseItem.getCreatedDate()));
        timeInfo.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.TERTIARY);

        HorizontalLayout cardFooter = new HorizontalLayout(author, timeInfo);
        cardFooter.setWidthFull();
        cardFooter.setJustifyContentMode(JustifyContentMode.BETWEEN);
        cardFooter.addClassName(LumoUtility.Margin.Top.MEDIUM);
        card.add(cardFooter);

        addStatusTheme(card, caseItem.getStatus());
        return card;
    }

    private Checkbox createSubtaskCheckbox(Subtask subtask, Case parentCase, ProgressBar progressBar) {
        Checkbox checkbox = new Checkbox(subtask.getTask(), subtask.isCompleted());

        Runnable updateStyle = () -> {
            if (checkbox.getValue()) {
                checkbox.getStyle().set("text-decoration", "line-through").set("color", "var(--lumo-secondary-text-color)");
            } else {
                checkbox.getStyle().remove("text-decoration").remove("color");
            }
        };
        updateStyle.run();

        checkbox.addValueChangeListener(event -> {
            subtask.setCompleted(event.getValue());
            caseRepository.save(parentCase);
            updateStyle.run();
            updateProgressBar(progressBar, parentCase);
            Notification.show("Stanje podnaloge posodobljeno.", 1000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

        return checkbox;
    }

    /**
     * Ustvari UI elemente za dodajanje nove podnaloge.
     */
    private HorizontalLayout createAddSubtaskControls(Case caseItem, VerticalLayout checkboxLayout, ProgressBar progressBar) {
        Button toggleAddSubtaskBtn = new Button("Dodaj podnalogo", new Icon(VaadinIcon.PLUS_CIRCLE));
        toggleAddSubtaskBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);

        TextField newSubtaskField = new TextField();
        newSubtaskField.setPlaceholder("Nova podnaloga...");
        newSubtaskField.setWidth("180px");

        Button confirmAddBtn = new Button(new Icon(VaadinIcon.CHECK));
        confirmAddBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        HorizontalLayout addSubtaskRow = new HorizontalLayout(newSubtaskField, confirmAddBtn);
        addSubtaskRow.setVisible(false);
        addSubtaskRow.setAlignItems(Alignment.BASELINE);

        toggleAddSubtaskBtn.addClickListener(e -> {
            addSubtaskRow.setVisible(true);
            toggleAddSubtaskBtn.setVisible(false);
            newSubtaskField.focus();
        });

        confirmAddBtn.addClickListener(e -> {
            String newTaskText = newSubtaskField.getValue();
            if (newTaskText != null && !newTaskText.trim().isEmpty()) {
                Subtask newSubtask = new Subtask();
                newSubtask.setTask(newTaskText);
                newSubtask.setCompleted(false);

                if (caseItem.getSubtasks() == null) {
                    caseItem.setSubtasks(new ArrayList<>());
                }
                caseItem.getSubtasks().add(newSubtask);
                caseRepository.save(caseItem);

                // --- POPRAVEK: CILJANO DODAJANJE NAMESTO OSVEŽEVANJA CELOTNE PLOŠČE ---
                Checkbox newCheckbox = createSubtaskCheckbox(newSubtask, caseItem, progressBar);
                checkboxLayout.add(newCheckbox); // Dodamo nov checkbox v njegov layout
                updateProgressBar(progressBar, caseItem); // Posodobimo progress bar

                newSubtaskField.clear();
                addSubtaskRow.setVisible(false);
                toggleAddSubtaskBtn.setVisible(true);
            }
        });

        HorizontalLayout wrapper = new HorizontalLayout(toggleAddSubtaskBtn, addSubtaskRow);
        wrapper.setAlignItems(Alignment.CENTER);
        return wrapper;
    }

    private ProgressBar createSubtaskProgressBar(Case caseItem) {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setWidthFull();
        updateProgressBar(progressBar, caseItem);
        return progressBar;
    }

    private void updateProgressBar(ProgressBar progressBar, Case caseItem) {
        if (caseItem.getSubtasks() == null || caseItem.getSubtasks().isEmpty()) {
            progressBar.setVisible(false);
            return;
        }
        progressBar.setVisible(true);
        long total = caseItem.getSubtasks().size();
        long completed = caseItem.getSubtasks().stream().filter(Subtask::isCompleted).count();
        progressBar.setValue((double) completed / total);
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

    private String formatTimeAgo(LocalDateTime createdDate) {
        if (createdDate == null) return "";
        Duration duration = Duration.between(createdDate, LocalDateTime.now());

        if (duration.toMinutes() < 1) return "pravkar";
        if (duration.toMinutes() < 60) return "pred " + duration.toMinutes() + " min";
        if (duration.toHours() < 24) return "pred " + duration.toHours() + " urami";
        if (duration.toDays() < 7) return "pred " + duration.toDays() + " dnevi";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", new Locale("sl", "SI"));
        return "dne " + createdDate.format(formatter);
    }
}
