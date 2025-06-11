package net.urosk.upravnikpredstavnik.security;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import net.urosk.upravnikpredstavnik.config.AppSecurityProperties;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.ui.views.AccessDeniedView;
import net.urosk.upravnikpredstavnik.ui.views.LoginView;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component

public class RouteAccessControl implements VaadinServiceInitListener {

    private final AppSecurityProperties appSecurityProperties;
    private final AuthenticatedUser authenticatedUser;

    public RouteAccessControl(AppSecurityProperties appSecurityProperties, AuthenticatedUser authenticatedUser) {
        this.appSecurityProperties = appSecurityProperties;
        this.authenticatedUser = authenticatedUser;
    }

    /**
     * Ta metoda registrira poslušalca, ki se sproži pred vsako navigacijo v aplikaciji.
     */
    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            uiEvent.getUI().addBeforeEnterListener(this::beforeEnter);
        });
    }

    /**
     * To je glavna metoda za preverjanje dostopa.
     * @param event Dogodek, ki vsebuje informacije o ciljni poti.
     */
    private void beforeEnter(BeforeEnterEvent event) {
        Class<?> targetView = event.getNavigationTarget();

        // Javno dostopne strani (npr. prijava, stran za napake) ne preverjamo.
        if (LoginView.class.equals(targetView) || AccessDeniedView.class.equals(targetView)) {
            return;
        }

        // Če uporabnik ni prijavljen, ga preusmerimo na prijavno stran.
        if (authenticatedUser.get().isEmpty()) {
            event.rerouteTo(LoginView.class);
            return;
        }

        // Na koncu preverimo, ali ima prijavljen uporabnik pravice za dostop do ciljnega pogleda.
        if (!isAccessGranted(targetView)) {
            // Če nima pravic, ga preusmerimo na stran "Dostop zavrnjen".
            event.rerouteTo(AccessDeniedView.class);
        }
    }

    /**
     * Preveri, ali ima trenutni uporabnik vsaj eno od vlog, ki so potrebne za dostop do pogleda.
     * @param viewClass Razred pogleda, do katerega se poskuša dostopati.
     * @return true, če je dostop dovoljen, sicer false.
     */
    private boolean isAccessGranted(Class<?> viewClass) {
        // Ker smo že preverili, vemo, da uporabnik obstaja.
        User currentUser = authenticatedUser.get().get();

        // Iz application.yml preberemo, katere vloge so potrebne za ta pogled.
        List<String> requiredRoles = appSecurityProperties.getViewAccess().get(viewClass.getName());

        // Če za pogled niso definirane nobene zahteve, dostop dovolimo.
        if (requiredRoles == null || requiredRoles.isEmpty()) {
            return true;
        }

        // Preberemo vloge, ki jih ima trenutni uporabnik.
        Set<String> userRoles = currentUser.getRoles();

        // Preverimo, ali se seznama vlog prekrivata (ali ima uporabnik vsaj eno od zahtevanih vlog).
        // Collections.disjoint vrne 'true', če seznama nimata skupnih elementov.
        // Zato uporabimo negacijo '!' za pravilen rezultat.
        return !Collections.disjoint(userRoles, requiredRoles);
    }
}