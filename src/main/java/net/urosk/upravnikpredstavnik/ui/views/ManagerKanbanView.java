package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import net.urosk.upravnikpredstavnik.data.Status;
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
    private final Map<Status, VerticalLayout> statusColumns = new HashMap<>();

    public ManagerKanbanView(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
        setSizeFull();
        setSpacing(true);
        addClassName("kanban-board");

        for (Status status : Status.values()) {
            VerticalLayout column = createStatusColumn(status);
            statusColumns.put(status, column);
            add(column);
        }

        loadAndDisplayCases();
    }

    private VerticalLayout createStatusColumn(Status status) {
        H3 title = new H3(status.name().replace("_", " "));
        VerticalLayout column = new VerticalLayout(title);
        column.addClassName("kanban-column");

        DropTarget<VerticalLayout> dropTarget = DropTarget.create(column);
        dropTarget.setActive(true);

        dropTarget.addDropListener(event -> {
            if (event.getDragData().isEmpty()) return;

            Optional<Component> dragSourceOpt = event.getDragSourceComponent();
            if (dragSourceOpt.isEmpty()) return;

            Component draggedComponent = dragSourceOpt.get();
            if (!(draggedComponent instanceof Card draggedCard)) return;

            event.getDragData().flatMap(data -> caseRepository.findById((String) data)).ifPresent(caseToUpdate -> {
                if (!column.equals(draggedCard.getParent().orElse(null))) {
                    caseToUpdate.setStatus(status);
                    caseRepository.save(caseToUpdate);
                    draggedCard.getParent().ifPresent(parent -> ((VerticalLayout) parent).remove(draggedCard));
                    column.add(draggedCard);
                    addStatusTheme(draggedCard, status);
                }
            });
        });

        return column;
    }

    private void loadAndDisplayCases() {
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

        Span title = new Span(caseItem.getTitle());
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);

        Span description = new Span(caseItem.getDescription());
        description.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.SMALL);

        Span author = new Span(caseItem.getAuthor().getName());
        author.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.TERTIARY);
        HorizontalLayout authorInfo = new HorizontalLayout(new Icon(VaadinIcon.USER), author);
        authorInfo.setAlignItems(Alignment.CENTER);
        authorInfo.addClassName(LumoUtility.Margin.Top.MEDIUM);

        Span timeInfo = new Span(formatTimeAgo(caseItem.getCreatedDate()));
        timeInfo.addClassNames(LumoUtility.FontSize.XXSMALL, LumoUtility.TextColor.TERTIARY);

        VerticalLayout subtaskLayout = new VerticalLayout();
        subtaskLayout.setPadding(false);
        subtaskLayout.setSpacing(false);
        subtaskLayout.setMargin(false);

        if (caseItem.getSubtasks() != null && !caseItem.getSubtasks().isEmpty()) {
            caseItem.getSubtasks().forEach(subtask -> {
                Checkbox checkbox = new Checkbox(subtask.getTask(), subtask.isCompleted());
                checkbox.setReadOnly(true);
                subtaskLayout.add(checkbox);
            });
        }

        // Gumb za odpiranje vnosnega polja
        Button toggleAddSubtaskBtn = new Button(new Icon(VaadinIcon.PLUS_CIRCLE));
        toggleAddSubtaskBtn.getElement().getStyle().set("margin-top", "0.5rem");
        toggleAddSubtaskBtn.getElement().getStyle().set("padding", "0.2rem");
        toggleAddSubtaskBtn.setTooltipText("Dodaj podnalogo");

        // Vnosno polje + potrditveni gumb (privzeto skrito)
        TextField newSubtaskField = new TextField();
        newSubtaskField.setPlaceholder("Nova podnaloga");
        newSubtaskField.setWidth("150px");
        newSubtaskField.getStyle().set("font-size", "var(--lumo-font-size-xs)");

        Button confirmAddBtn = new Button(new Icon(VaadinIcon.CHECK));
        confirmAddBtn.getElement().getStyle().set("padding", "0.2rem");
        confirmAddBtn.addThemeName("primary");

        HorizontalLayout addSubtaskRow = new HorizontalLayout(newSubtaskField, confirmAddBtn);
        addSubtaskRow.setVisible(false);
        addSubtaskRow.setSpacing(true);
        addSubtaskRow.setPadding(false);
        addSubtaskRow.setAlignItems(Alignment.BASELINE);

        toggleAddSubtaskBtn.addClickListener(e -> {
            addSubtaskRow.setVisible(true);
            toggleAddSubtaskBtn.setVisible(false);
            newSubtaskField.focus();
        });

        confirmAddBtn.addClickListener(e -> {
            String newTask = newSubtaskField.getValue();
            if (newTask != null && !newTask.trim().isEmpty()) {
                Subtask subtask = new Subtask();
                subtask.setTask(newTask);
                subtask.setCompleted(false);
                caseItem.getSubtasks().add(subtask);
                caseRepository.save(caseItem);

                Checkbox newCheckbox = new Checkbox(newTask, false);
                newCheckbox.setReadOnly(true);
                subtaskLayout.add(newCheckbox);
                newSubtaskField.clear();

                addSubtaskRow.setVisible(false);
                toggleAddSubtaskBtn.setVisible(true);
            }
        });

        card.setTitle(title);
        card.add(description, timeInfo, subtaskLayout, toggleAddSubtaskBtn, addSubtaskRow, authorInfo);
        addStatusTheme(card, caseItem.getStatus());
        return card;
    }

    private void addStatusTheme(Card card, Status status) {
        card.removeClassName("status-predlog");
        card.removeClassName("status-v-delu");
        card.removeClassName("status-zakljuceno");

        String theme = switch (status) {
            case PREDLOG, V_PREGLEDU -> "status-predlog";
            case POTRJENO, V_DELU -> "status-v-delu";
            case ZAKLJUCENO -> "status-zakljuceno";
        };
        card.addClassName(theme);
    }

    private String formatTimeAgo(LocalDateTime createdDate) {
        if (createdDate == null) return "";
        Duration duration = Duration.between(createdDate, LocalDateTime.now());

        if (duration.toMinutes() < 1) return "pravkar";
        if (duration.toMinutes() < 60) return "pred " + duration.toMinutes() + " minutami";
        if (duration.toHours() < 24) return "pred " + duration.toHours() + " urami";
        if (duration.toDays() < 7) return "pred " + duration.toDays() + " dnevi";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", new Locale("sl", "SI"));
        return "dne " + createdDate.format(formatter);
    }
}
