// POPRAVLJENA VERZIJA: src/main/java/net/urosk/upravnikpredstavnik/ui/views/LoginView.java
package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;

@Route("login")
@PageTitle("Prijava | Upravnik-Predstavnik")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private static final String OAUTH_URL = "/oauth2/authorization/google";
    private final AuthenticatedUser authenticatedUser;

    public LoginView(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("Sistem za upravljanje stavbe");
        title.getStyle().set("color", "white").set("text-shadow", "1px 1px 2px black");

        Anchor loginLink = new Anchor(OAUTH_URL, "Prijava z Google računom");

        // --- KLJUČNA SPREMEMBA JE TUKAJ ---
        loginLink.setRouterIgnore(true); // To pove Vaadinu, naj ne upravlja te povezave
        // ------------------------------------

        loginLink.getElement().getStyle()
                .set("color", "white")
                .set("background-color", "rgba(0, 0, 0, 0.5)")
                .set("padding", "1rem 1.5rem")
                .set("border-radius", "8px")
                .set("border", "1px solid white")
                .set("text-decoration", "none")
                .set("font-weight", "bold");

        add(title, loginLink);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticatedUser.get().isPresent()) {
            event.forwardTo("");
        }
    }
}