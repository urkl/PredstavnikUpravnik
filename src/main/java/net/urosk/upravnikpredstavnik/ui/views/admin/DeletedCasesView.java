package net.urosk.upravnikpredstavnik.ui.views.admin;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import net.urosk.upravnikpredstavnik.config.AppProcessProperties;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;
import net.urosk.upravnikpredstavnik.service.AuditService;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class DeletedCasesView extends VerticalLayout {

    private final CaseRepository caseRepository;

    private final AuditService auditService;
    private final AuthenticatedUser authenticatedUser;
    private final AppProcessProperties processProperties ;

    private final Grid<Case> grid = new Grid<>(Case.class, false);

    public DeletedCasesView(CaseRepository caseRepository, AuditService auditService, AuthenticatedUser authenticatedUser, AppProcessProperties processProperties) {
        this.caseRepository = caseRepository;

        this.auditService = auditService;
        this.authenticatedUser = authenticatedUser;
        this.processProperties = processProperties;

        setSizeFull();
        setSpacing(true);

        configureGrid();
        add(grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(Case::getTitle).setHeader("Naslov").setSortable(true);
        grid.addColumn(c -> c.getAuthor().getName()).setHeader("Avtor").setSortable(true);
        grid.addColumn(c -> c.getLastModifiedDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                .setHeader("Datum izbrisa").setSortable(true);
        grid.addColumn(new ComponentRenderer<>(this::createRestoreButton)).setHeader("Dejanja");
    }

    private Button createRestoreButton(Case caseItem) {
        Button restoreButton = new Button("Povrni", new Icon(VaadinIcon.RECYCLE), click -> confirmRestoreCase(caseItem));
        restoreButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
        restoreButton.setTooltipText("Povrni zadevo v prvotno stanje");
        return restoreButton;
    }

    private void confirmRestoreCase(Case caseToRestore) {
        ConfirmDialog dialog = new ConfirmDialog(
                "Potrditev obnove",
                "Ali res želite obnoviti zadevo '" + caseToRestore.getTitle() + "'? Status bo nastavljen na privzetega.",
                "Obnovi", e -> restoreCase(caseToRestore),
                "Prekliči", e -> {}
        );
        dialog.setConfirmButtonTheme(ButtonVariant.LUMO_SUCCESS.getVariantName());
        dialog.open();
    }

    private void restoreCase(Case caseToRestore) {
        String restoredStatus = processProperties.getDefaultStatus();
        caseToRestore.setStatus(restoredStatus);
        caseRepository.save(caseToRestore);

        String userEmail = authenticatedUser.get().map(User::getEmail).orElse("SYSTEM");
        String details = "Zadeva obnovljena. Nov status: '" + processProperties.getStatuses().get(restoredStatus) + "'";
        auditService.log("ZADEVA OBNOVLJENA", Case.class, caseToRestore.getId(), details, userEmail);

        Notification.show("Zadeva '" + caseToRestore.getTitle() + "' uspešno obnovljena.", 3000, Notification.Position.TOP_CENTER);
        refreshGrid();
    }

    private void refreshGrid() {
        List<Case> deletedCases = caseRepository.findByStatus("DELETED");
        grid.setItems(deletedCases);
    }
}