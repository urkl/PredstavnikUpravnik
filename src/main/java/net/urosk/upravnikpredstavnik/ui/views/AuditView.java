// FILE: src/main/java/net/urosk/upravnikpredstavnik/ui/views/AuditView.java
package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import net.urosk.upravnikpredstavnik.data.entity.AuditLog;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.repository.AuditLogRepository;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Route(value = "audit", layout = MainLayout.class)
@PageTitle("Zgodovina aktivnosti")
@PermitAll
public class AuditView extends VerticalLayout {

    private final Grid<AuditLog> grid = new Grid<>(AuditLog.class);
    private final AuditLogRepository auditLogRepository;
    private final CaseRepository caseRepository; // <-- DODAN REPOZITORIJ

    // SPREMENJENO: Dodan CaseRepository v konstruktor
    public AuditView(AuditLogRepository auditLogRepository, CaseRepository caseRepository) {
        this.auditLogRepository = auditLogRepository;
        this.caseRepository = caseRepository; // <-- Shranimo repozitorij
        addClassName("audit-view");
        setSizeFull();
        setPadding(true);

        H2 title = new H2("Zgodovina aktivnosti");
        title.addClassName("title");

        configureGrid();

        grid.setItems(query ->
                auditLogRepository.findAll(
                        PageRequest.of(query.getPage(), query.getPageSize(), Sort.by(Sort.Direction.DESC, "timestamp"))
                ).stream()
        );

        add(title, grid);
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.setPageSize(50);
        grid.addColumn(new ComponentRenderer<>(this::createLogEntryComponent))
                .setHeader("Aktivnost");
    }

    private Component createLogEntryComponent(AuditLog log) {
        Icon icon = getIconForAction(log.getAction());
        icon.getStyle().set("margin-right", "var(--lumo-space-m)");

        Span userAction = new Span(log.getUserEmail() + " " + log.getAction().toLowerCase().replace("_", " "));
        userAction.getStyle().set("font-weight", "bold");

        // --- SPREMEMBA: Ustvarimo vsebnik za podrobnosti in dodamo kontekst ---
        VerticalLayout detailsContainer = new VerticalLayout();
        detailsContainer.setPadding(false);
        detailsContainer.setSpacing(false);
        detailsContainer.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        // Prikažemo originalne podrobnosti
        Span primaryDetails = new Span(log.getDetails());
        detailsContainer.add(primaryDetails);

        // Če se zapis nanaša na zadevo (Case), dodamo povezavo z naslovom
        if ("Case".equals(log.getEntityType()) && log.getEntityId() != null) {
            caseRepository.findById(log.getEntityId()).ifPresent(aCase -> {
                RouterLink caseLink = new RouterLink("Zadeva: '" + aCase.getTitle() + "'", CaseDetailView.class, aCase.getId());
                caseLink.getStyle().set("font-style", "italic");
                detailsContainer.add(caseLink);
            });
        }
        // --- KONEC SPREMEMBE ---

        Div textContent = new Div(userAction, detailsContainer);

        Span time = new Span(formatTimeAgo(log.getTimestamp()));
        time.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.TERTIARY);
        time.getStyle().set("margin-left", "auto");

        HorizontalLayout entryLayout = new HorizontalLayout(icon, textContent, time);
        entryLayout.setWidthFull();
        entryLayout.setAlignItems(Alignment.CENTER);
        return entryLayout;
    }

    // Ostale metode ostanejo enake...
    private Icon getIconForAction(String action) {
        if (action == null) return new Icon(VaadinIcon.INFO_CIRCLE);
        return switch (action.toUpperCase()) {
            case "SPREMEMBA STATUSA" -> new Icon(VaadinIcon.EXCHANGE);
            case "USTVARIL ZADEVO" -> new Icon(VaadinIcon.PLUS_CIRCLE);
            case "POSODOBIL ZADEVO" -> new Icon(VaadinIcon.PENCIL);
            case "NOV KOMENTAR" -> new Icon(VaadinIcon.COMMENT);
            case "DODANA DATOTEKA" -> new Icon(VaadinIcon.FILE_ADD);
            case "ZBRISANA DATOTEKA" -> new Icon(VaadinIcon.FILE_REMOVE);
            case "DODAJANJE PODNALOGE" -> new Icon(VaadinIcon.LIST_UL);
            case "POSODOBITEV PODNALOGE" -> new Icon(VaadinIcon.CHECK_SQUARE_O);
            case "ZADEVA ZBRISANA" -> new Icon(VaadinIcon.TRASH);
            case "OBNOVITEV ZADEVE" -> new Icon(VaadinIcon.RECYCLE);
            default -> new Icon(VaadinIcon.INFO_CIRCLE);
        };
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        Duration duration = Duration.between(dateTime, LocalDateTime.now());

        if (duration.toMinutes() < 1) return "pravkar";
        if (duration.toMinutes() < 60) return "pred " + duration.toMinutes() + " min";
        if (duration.toHours() < 24) return "pred " + duration.toHours() + " h";
        if (duration.toDays() < 7) return "pred " + duration.toDays() + " dnevi";

        return dateTime.format(DateTimeFormatter.ofPattern("d. M.yyyy 'ob' HH:mm"));
    }
}