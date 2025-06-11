package net.urosk.upravnikpredstavnik.security;

import com.vaadin.flow.spring.security.AuthenticationContext;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class AuthenticatedUser {

    private final UserRepository userRepository;
    private final AuthenticationContext authenticationContext;

    public AuthenticatedUser(AuthenticationContext authenticationContext, UserRepository userRepository) {
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;
    }

    public Optional<User> get() {
        // Najprej poskusimo pridobiti uporabnika kot OidcUser/OAuth2User (za Google prijavo).
        Optional<User> oauthUser = authenticationContext.getAuthenticatedUser(OAuth2User.class)
                .map(principal -> userRepository.findByEmail(principal.getAttribute("email")))
                .orElse(Optional.empty());

        if (oauthUser.isPresent()) {
            return oauthUser;
        }

        // Če to ne uspe, poskusimo pridobiti uporabnika kot UserDetails (za "Remember Me" prijavo).
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .map(principal -> userRepository.findByEmail(principal.getUsername()))
                .orElse(Optional.empty());
    }

    public void logout() {
        authenticationContext.logout();
    }
}