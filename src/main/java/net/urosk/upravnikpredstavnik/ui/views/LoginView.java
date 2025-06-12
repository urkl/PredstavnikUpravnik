package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Paragraph; // NOV UVOZ
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;

@Route("login")
@PageTitle("Prijava | BlokApp")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private static final String OAUTH_URL = "/oauth2/authorization/google";
    private final AuthenticatedUser authenticatedUser;

    public LoginView(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1();
        Span titleBadge = new Span("Dobrodošli v BlokApp!");
        titleBadge.addClassName("title-badge");
        title.add(titleBadge);

        // NOV DODATEK: Podnapis (tagline)
        Paragraph tagline = new Paragraph("Aplikacija za urejeno in prijazno komunikacijo med stanovalci, predstavniki in upravniki.");
        tagline.getStyle()
                .set("color", "white") // Bela barva teksta za dober kontrast
                .set("text-shadow", "1px 1px 2px black") // Majhna senca za boljši izstop na sliki ozadja
                .set("font-size", "var(--lumo-font-size-l)") // Povečana velikost pisave
                .set("margin-top", "var(--lumo-space-m)") // Razmik nad napisom za prijavo
                .set("text-align", "center") // Centriranje teksta
                .set("max-width", "80%"); // Omejitev širine, da se ne razteza preveč

        Anchor loginLink = new Anchor(OAUTH_URL, "Prijava z Google računom");
        loginLink.setRouterIgnore(true);
        loginLink.getElement().getStyle()
                .set("color", "white")
                .set("background-color", "rgba(0, 0, 0, 0.5)")
                .set("padding", "1rem 1.5rem")
                .set("border-radius", "8px")
                .set("border", "1px solid white")
                .set("text-decoration", "none")
                .set("font-weight", "bold");

        // Dodajte vse elemente v VerticalLayout
        add(title, tagline, loginLink);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticatedUser.get().isPresent()) {
            event.forwardTo("");
        }
    }
}