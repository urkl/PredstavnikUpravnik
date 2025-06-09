// FILE: src/main/java/net/urosk/upravnikpredstavnik/ui/views/ManagerKanbanView.java
package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.urosk.upravnikpredstavnik.data.Status;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;

@Route(value = "kanban", layout = MainLayout.class)
@PageTitle("Kanban Pregled")
@RolesAllowed({"UPRAVNIK", "PREDSTAVNIK"})
public class ManagerKanbanView extends HorizontalLayout {

    private final CaseRepository caseRepository;

    public ManagerKanbanView(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
        setSizeFull();
        setSpacing(true);

        // Ustvari stolpce za vsak status
        for (Status status : Status.values()) {
            add(createStatusColumn(status));
        }
    }

    private VerticalLayout createStatusColumn(Status status) {
        VerticalLayout column = new VerticalLayout();
        column.setSpacing(true);
        column.add(new H3(status.name().replace("_", " ")));

        // Poišči zadeve za ta status in jih prikaži kot kartice
        caseRepository.findByStatus(status).forEach(caseItem -> {
            column.add(createCaseCard(caseItem));
        });

        return column;
    }

    private VerticalLayout createCaseCard(Case caseItem) {
        VerticalLayout card = new VerticalLayout();
        card.getStyle()
                .set("border", "1px solid #ddd")
                .set("border-radius", "8px")
                .set("padding", "1rem");
        card.add(caseItem.getTitle());
        if(caseItem.getAssignedTo() != null) {
            card.add("Dodeljeno: " + caseItem.getAssignedTo().getName());
        }
        return card;
    }
}