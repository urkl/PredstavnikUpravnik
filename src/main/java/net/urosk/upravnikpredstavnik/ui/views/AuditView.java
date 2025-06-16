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
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import net.urosk.upravnikpredstavnik.data.entity.AuditLog;
import net.urosk.upravnikpredstavnik.data.repository.AuditLogRepository;
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

    public AuditView(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
        addClassName("audit-view");
        setSizeFull();
        setPadding(true);

        H2 title = new H2("Zgodovina aktivnosti");
        title.addClassName("title");

        configureGrid();

        // === POPRAVEK: Uporaba Lazy Loading-a za paginacijo in sortiranje ===
        grid.setItems(query ->
                auditLogRepository.findAll(
                        PageRequest.of(query.getPage(), query.getPageSize(), Sort.by(Sort.Direction.DESC, "timestamp"))
                ).stream()
        );
        // ===================================================================

        add(title, grid);
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();

        // Povečamo velikost strani za boljšo preglednost
        grid.setPageSize(50);

        grid.addColumn(new ComponentRenderer<>(this::createLogEntryComponent))
                .setHeader("Aktivnost");
    }

    private Component createLogEntryComponent(AuditLog log) {
        Icon icon = getIconForAction(log.getAction());
        icon.getStyle().set("margin-right", "var(--lumo-space-m)");

        Span userAction = new Span(log.getUserEmail() + " " + log.getAction().toLowerCase().replace("_", " "));
        userAction.getStyle().set("font-weight", "bold");

        Span details = new Span(log.getDetails());
        details.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        Div textContent = new Div(userAction, new Div(details));

        Span time = new Span(formatTimeAgo(log.getTimestamp()));
        time.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.TERTIARY);
        time.getStyle().set("margin-left", "auto");

        HorizontalLayout entryLayout = new HorizontalLayout(icon, textContent, time);
        entryLayout.setWidthFull();
        entryLayout.setAlignItems(Alignment.CENTER);
        return entryLayout;
    }

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
            case "ZADEVA ZBRISANA" -> new Icon(VaadinIcon.TRASH); // <-- DODANO
            case "ZADEVA OBNOVLJENA" -> new Icon(VaadinIcon.RECYCLE); // <-- DODANO

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

        return dateTime.format(DateTimeFormatter.ofPattern("d. M. yyyy 'ob' HH:mm"));
    }
}