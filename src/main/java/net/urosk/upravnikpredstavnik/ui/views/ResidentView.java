package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import net.urosk.upravnikpredstavnik.config.AppProcessProperties;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;
import net.urosk.upravnikpredstavnik.ui.util.CaseFormUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Moje Zadeve")
public class ResidentView extends VerticalLayout {
    private final CaseRepository caseRepository;
    private final AuthenticatedUser authenticatedUser;
    private final AppProcessProperties appProcessProperties;

    private final VerticalLayout caseCardsLayout = new VerticalLayout();

    public ResidentView(CaseRepository caseRepository, AuthenticatedUser authenticatedUser, AppProcessProperties appProcessProperties) {
        this.caseRepository = caseRepository;
        this.authenticatedUser = authenticatedUser;
        this.appProcessProperties = appProcessProperties;

        setSizeFull();
        setPadding(true);
        setSpacing(false); // Odstranimo privzeti razmik

        // Obrazec za dodajanje
        VerticalLayout form = CaseFormUtils.createResidentCaseForm(
                caseRepository,
                authenticatedUser,
                appProcessProperties.getDefaultStatus(),
                this::refreshCaseList // Pošljemo metodo za osvežitev
        );
        form.setMaxWidth("800px");

        caseCardsLayout.setPadding(false);
        caseCardsLayout.setSpacing(true);
        caseCardsLayout.setMaxWidth("800px");

        // Vsebino centriramo
        add(form, caseCardsLayout);
        setAlignItems(Alignment.CENTER);

        refreshCaseList(null); // Začetno nalaganje
    }

    private void refreshCaseList(Case newCase) {
        // Počistimo obstoječe kartice in jih naložimo na novo
        caseCardsLayout.removeAll();
        authenticatedUser.get().ifPresent(user -> {
            List<Case> cases = caseRepository.findFirst10ByAuthorIdOrderByCreatedDateDesc(user.getId());
            cases.forEach(caseItem -> caseCardsLayout.add(createCaseCard(caseItem)));
        });
    }

    private VerticalLayout createCaseCard(Case caseItem) {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "1rem");

        // Glava kartice z naslovom, statusom in gumbom za brisanje
        H4 title = new H4(caseItem.getTitle());
        title.getStyle().set("margin", "0");

        // Status z barvo
        Span statusBadge = new Span(appProcessProperties.getStatuses().get(caseItem.getStatus()));
        statusBadge.getElement().getThemeList().add("badge");
        String color = appProcessProperties.getStatuses().get(caseItem.getStatus()).getColor();
        statusBadge.getStyle().set("background-color", color).set("color", "white");

        HorizontalLayout header = new HorizontalLayout(title, statusBadge);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);

        if ("PREDLOG".equals(caseItem.getStatus())) {
            Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteBtn.getStyle().set("margin-left", "auto");
            deleteBtn.addClickListener(e -> {
                caseRepository.deleteById(caseItem.getId());
                Notification.show("Zadeva izbrisana.");
                refreshCaseList(null);
            });
            header.add(deleteBtn);
            header.expand(statusBadge);
        }

        // Opis in datum
        Span description = new Span(caseItem.getDescription());
        description.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d. M. yyyy 'ob' HH:mm");
        Span date = new Span("Oddano: " + caseItem.getCreatedDate().format(formatter));
        date.getStyle().set("color", "var(--lumo-tertiary-text-color)").set("font-size", "var(--lumo-font-size-xs)");

        card.add(header, description, date);
        return card;
    }
}