// FILE: src/main/java/net/urosk/upravnikpredstavnik/security/AuthenticatedUser.java
package net.urosk.upravnikpredstavnik.security;

import com.vaadin.flow.spring.security.AuthenticationContext;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
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
        return authenticationContext.getAuthenticatedUser(OAuth2User.class)
                .map(oauthUser -> userRepository.findByEmail(oauthUser.getAttribute("email")))
                .orElse(Optional.empty());
    }

    public void logout() {
        authenticationContext.logout();
    }
}