package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "/", layout = MainLayout.class) // Privzeta stran po prijavi
@PageTitle("Domov")
@AnonymousAllowed
public class HomeView extends VerticalLayout {

    public HomeView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        add(new com.vaadin.flow.component.html.H1("Dobrodošli v sistemu Upravnik & Predstavnik!"));

        // Tukaj lahko dodate več vsebine, kot so povezave do drugih strani ali informacije
    }
}
