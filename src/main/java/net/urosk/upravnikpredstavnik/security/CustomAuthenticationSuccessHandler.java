
package net.urosk.upravnikpredstavnik.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.urosk.upravnikpredstavnik.data.Role;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Value("${app.admin-email}")
    private String adminEmail;

    public CustomAuthenticationSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // Iz prijavnega žetona pridobimo podatke o uporabniku
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");

        // Preverimo, ali uporabnik obstaja, in ga ustvarimo, če ne
        userRepository.findByEmail(email).or(() -> {
            System.out.println("!!! Uporabnik z emailom " + email + " ne obstaja. Ustvarjam novega... !!!");
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(oauthUser.getAttribute("name"));
            newUser.setActivated(true);

            if (adminEmail.equalsIgnoreCase(email)) {
                newUser.setRole(Role.PREDSTAVNIK);
            } else {
                newUser.setRole(Role.STANOVALEC);
            }

            return Optional.of(userRepository.save(newUser));
        });

        // Po končani logiki preusmerimo uporabnika na glavno stran
        response.sendRedirect("/");
    }
}