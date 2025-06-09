// POSODOBLJENA VERZIJA: src/main/java/net/urosk/upravnikpredstavnik/ui/views/ResidentView.java
package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;

@Route(value = "/resident", layout = MainLayout.class) // Privzeta stran po prijavi
@PageTitle("Moje Zadeve")
// --- SPREMEMBA TUKAJ: Dovolimo dostop vsem prijavljenim vlogam ---
@RolesAllowed({"STANOVALEC", "UPRAVNIK", "PREDSTAVNIK"})
public class ResidentView extends VerticalLayout {

    private final CaseRepository caseRepository;
    private final AuthenticatedUser authenticatedUser;

    public ResidentView(CaseRepository caseRepository, AuthenticatedUser authenticatedUser) {
        this.caseRepository = caseRepository;
        this.authenticatedUser = authenticatedUser;

        Grid<Case> grid = new Grid<>(Case.class);
        grid.setColumns("title", "description", "status", "lastModifiedDate");
        add(grid);

        // Naloži zadeve samo za trenutnega uporabnika
        authenticatedUser.get().ifPresent(user -> {
            grid.setItems(caseRepository.findByAuthorId(user.getId()));
        });
    }
}