// POSODOBLJENA VERZIJA: src/main/java/net/urosk/upravnikpredstavnik/ui/views/ResidentView.java
package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import net.urosk.upravnikpredstavnik.data.Status;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;
import net.urosk.upravnikpredstavnik.ui.util.CaseFormUtils;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Moje Zadeve")
@PermitAll
public class ResidentView extends VerticalLayout {

    private final CaseRepository caseRepository;
    private final AuthenticatedUser authenticatedUser;

    public ResidentView(CaseRepository caseRepository, AuthenticatedUser authenticatedUser) {
        this.caseRepository = caseRepository;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setSpacing(true);
        setPadding(true);
        getStyle().set("background-color", "rgba(255, 255, 255, 0.9)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 16px rgba(0,0,0,0.2)")
                .set("padding", "1rem");

        Grid<Case> grid = new Grid<>(Case.class);
        grid.setColumns("title", "description", "status", "lastModifiedDate");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        // Dodamo gumb za izbris če je status PREDLOG
        grid.addComponentColumn(item -> {
            if (item.getStatus() == Status.PREDLOG) {
                Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
                deleteBtn.getElement().setAttribute("title", "Izbriši zadevo");
                deleteBtn.addClickListener(e -> {
                    caseRepository.deleteById(item.getId());
                    authenticatedUser.get().ifPresent(user -> {
                        grid.setItems(caseRepository.findByAuthorId(user.getId()));
                    });
                    Notification.show("Zadeva izbrisana.");
                });
                return deleteBtn;
            }
            return new Div();
        }).setHeader("").setAutoWidth(true);

        // Dodamo obrazec preko util razreda
        add(CaseFormUtils.createResidentCaseForm(caseRepository, authenticatedUser, grid));
        add(grid);

        authenticatedUser.get().ifPresent(user -> {
            grid.setItems(caseRepository.findByAuthorId(user.getId()));
        });
    }
}