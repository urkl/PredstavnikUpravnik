// FILE: src/main/java/net/urosk/upravnikpredstavnik/service/ActiveUserService.java
package net.urosk.upravnikpredstavnik.service;

import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import net.urosk.upravnikpredstavnik.security.CustomOidcUser; // <-- NOV UVOZ
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActiveUserService {

    private final SessionRegistry sessionRegistry;
    private final UserRepository userRepository;

    public ActiveUserService(SessionRegistry sessionRegistry, UserRepository userRepository) {
        this.sessionRegistry = sessionRegistry;
        this.userRepository = userRepository;
    }

    /**
     * Vrne seznam vseh trenutno aktivnih (prijavljenih) uporabnikov.
     */
    public List<User> getActiveUsers() {
        return sessionRegistry.getAllPrincipals().stream()
                // Izločimo seje, ki so potekle, ali anonimne uporabnike
                .filter(principal -> !sessionRegistry.getAllSessions(principal, false).isEmpty())
                // Preslikamo "principal" objekt v našo User entiteto
                .map(this::mapPrincipalToUser)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct() // Vsakega uporabnika prikažemo samo enkrat
                .collect(Collectors.toList());
    }

    /**
     * Pomožna metoda, ki iz Spring Security objekta (principal)
     * pridobi uporabniško ime (e-pošto) in poišče uporabnika v naši bazi.
     * Sedaj pravilno obravnava tudi uporabnike, prijavljene z Googlom.
     */
    private Optional<User> mapPrincipalToUser(Object principal) {
        String email = null;

        // Najprej preverimo za naš specifičen tip uporabnika iz Google prijave.
        if (principal instanceof CustomOidcUser) {
            email = ((CustomOidcUser) principal).getEmail();
        }
        // Nato za standardno prijavo z uporabniškim imenom/geslom.
        else if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        }
        // Splošni OidcUser, če bi ga kdaj uporabili.
        else if (principal instanceof OidcUser) {
            email = ((OidcUser) principal).getEmail();
        }

        if (email != null) {
            return userRepository.findByEmail(email);
        }

        return Optional.empty();
    }
}