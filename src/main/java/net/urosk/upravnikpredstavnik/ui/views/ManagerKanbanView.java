// FILE: src/main/java/net/urosk/upravnikpredstavnik/ui/views/ManagerKanbanView.java
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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import net.urosk.upravnikpredstavnik.config.AppProcessProperties;
import net.urosk.upravnikpredstavnik.data.entity.Building;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.entity.Subtask;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;
import net.urosk.upravnikpredstavnik.ui.components.CommentsDialog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(value = "kanban", layout = MainLayout.class)
@PageTitle("Kanban Pregled")
@PermitAll
public class ManagerKanbanView extends HorizontalLayout {

    private final CaseRepository caseRepository;
    private final Map<String, VerticalLayout> statusColumns = new HashMap<>();
    private final AppProcessProperties processProperties;
    private final AuthenticatedUser authenticatedUser;

    public ManagerKanbanView(CaseRepository caseRepository, AppProcessProperties processProperties, AuthenticatedUser authenticatedUser) {
        this.caseRepository = caseRepository;
        this.processProperties = processProperties;
        this.authenticatedUser = authenticatedUser;
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
        title.addClassNames("title");
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
        statusColumns.values().forEach(column -> {
            column.getChildren()
                    .filter(component -> component instanceof Card)
                    .toList()
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
        cardHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        card.add(cardHeader);

        // --- VSEBINA KARTICE ---
        Span description = new Span(caseItem.getDescription());
        description.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.SMALL);
        card.add(description);

        if (caseItem.getBuildings() != null && !caseItem.getBuildings().isEmpty()) {
            HorizontalLayout buildingsLayout = new HorizontalLayout();
            buildingsLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            buildingsLayout.setSpacing(true);
            buildingsLayout.addClassNames(LumoUtility.Margin.Top.XSMALL, LumoUtility.Margin.Bottom.XSMALL);
            Icon buildingIcon = VaadinIcon.HOME_O.create();
            buildingIcon.setColor("var(--lumo-contrast-50pct)");
            buildingIcon.setSize("16px");
            String buildingNames = caseItem.getBuildings().stream().map(Building::getName).collect(Collectors.joining(", "));
            Span buildingsSpan = new Span(buildingNames);
            buildingsSpan.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);
            buildingsLayout.add(buildingIcon, buildingsSpan);
            card.add(buildingsLayout);
        }

        if (caseItem.getStartDate() != null || caseItem.getEndDate() != null) {
            HorizontalLayout datesLayout = new HorizontalLayout();
            datesLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            datesLayout.setSpacing(true);
            datesLayout.addClassNames(LumoUtility.Margin.Top.XSMALL, LumoUtility.Margin.Bottom.XSMALL);
            Icon calendarIcon = VaadinIcon.CALENDAR_O.create();
            calendarIcon.setColor("var(--lumo-contrast-50pct)");
            calendarIcon.setSize("16px");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.yyyy", new Locale("sl", "SI"));
            String dateText = "";
            if (caseItem.getStartDate() != null && caseItem.getEndDate() != null) {
                dateText = "Od " + caseItem.getStartDate().format(formatter) + " do " + caseItem.getEndDate().format(formatter);
            } else if (caseItem.getStartDate() != null) {
                dateText = "Začetek: " + caseItem.getStartDate().format(formatter);
            } else if (caseItem.getEndDate() != null) {
                dateText = "Konec: " + caseItem.getEndDate().format(formatter);
            }
            Span datesSpan = new Span(dateText);
            datesSpan.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);
            datesLayout.add(calendarIcon, datesSpan);
            card.add(datesLayout);
        }

        // --- PODNALOGE (SUBTASKS) ---
        VerticalLayout subtasksContainer = new VerticalLayout();
        subtasksContainer.setPadding(false);
        subtasksContainer.setSpacing(false);
        subtasksContainer.addClassName(LumoUtility.Margin.Top.MEDIUM);
        ProgressBar progressBar = createSubtaskProgressBar(caseItem);
        subtasksContainer.add(progressBar);
        VerticalLayout checkboxLayout = new VerticalLayout();
        checkboxLayout.setPadding(false);
        checkboxLayout.setSpacing(false);
        if (caseItem.getSubtasks() != null) {
            caseItem.getSubtasks().forEach(subtask -> checkboxLayout.add(createSubtaskCheckbox(subtask, caseItem, progressBar)));
        }
        subtasksContainer.add(checkboxLayout);
        HorizontalLayout addSubtaskControls = createAddSubtaskControls(caseItem, checkboxLayout, progressBar);
        subtasksContainer.add(addSubtaskControls);
        card.add(subtasksContainer);

        // --- NOGA KARTICE (POSODOBLJENA LOGIKA) ---
        Span author = new Span("Prijavil: " + caseItem.getAuthor().getName());
        author.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.TERTIARY);

        Button commentsButton = new Button(new Icon(VaadinIcon.COMMENTS_O));
        commentsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        int commentCount = caseItem.getComments() != null ? caseItem.getComments().size() : 0;
        commentsButton.setText(String.valueOf(commentCount));
        commentsButton.setTooltipText("Prikaži komentarje (" + commentCount + ")");
        commentsButton.addClickListener(e -> {
            CommentsDialog dialog = new CommentsDialog(caseItem, caseRepository, authenticatedUser,this::loadAndDisplayCases);

            dialog.open();
        });

        Span timeInfo = new Span(formatTimeAgo(caseItem.getCreatedDate()));
        timeInfo.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.TERTIARY);

        // Avtor na levi, ostalo na desni
        HorizontalLayout cardFooter = new HorizontalLayout(author, commentsButton, timeInfo);
        cardFooter.setAlignItems(FlexComponent.Alignment.CENTER);
        cardFooter.setWidthFull();
        // Pomaknemo gumb in čas na desno stran s praznim Div elementom, ki se razširi
        Div spacer = new Div();
        cardFooter.expand(spacer); // Div bo zavzel ves preostali prostor
        cardFooter.addComponentAtIndex(1, spacer); // Dodamo ga med avtorja in gumb

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
        addSubtaskRow.setAlignItems(FlexComponent.Alignment.BASELINE);

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

                Checkbox newCheckbox = createSubtaskCheckbox(newSubtask, caseItem, progressBar);
                checkboxLayout.add(newCheckbox);
                updateProgressBar(progressBar, caseItem);

                newSubtaskField.clear();
                addSubtaskRow.setVisible(false);
                toggleAddSubtaskBtn.setVisible(true);
            }
        });

        HorizontalLayout wrapper = new HorizontalLayout(toggleAddSubtaskBtn, addSubtaskRow);
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
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