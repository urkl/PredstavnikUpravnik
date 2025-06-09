// POPRAVLJENA VERZIJA: src/main/java/net/urosk/upravnikpredstavnik/security/CustomOAuth2UserService.java
package net.urosk.upravnikpredstavnik.security;

import net.urosk.upravnikpredstavnik.data.Role;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Value("${app.admin-email}")
    private String adminEmail;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);
        String email = oauthUser.getAttribute("email");

        // PRAVILNA LOGIKA: Rezultat operacije shranimo v 'appUser'.
        // Ta spremenljivka bo vedno vsebovala ali obstoječega ali na novo ustvarjenega uporabnika.
        User appUser = userRepository.findByEmail(email)
                .orElseGet(() -> {
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
                    return userRepository.save(newUser);
                });

        // Sedaj 'appUser' ne more biti null in ga lahko varno uporabimo.
        return new CustomOAuth2User(oauthUser, appUser);
    }
}