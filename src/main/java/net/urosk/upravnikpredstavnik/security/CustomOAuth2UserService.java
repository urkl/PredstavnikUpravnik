// POSODOBLJENA VERZIJA: src/main/java/net/urosk/upravnikpredstavnik/security/CustomOAuth2UserService.java
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

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    // 1. Vbrizgamo vrednost iz application.yml v spremenljivko
    @Value("${app.admin-email}")
    private String adminEmail;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);

        String email = oauthUser.getAttribute("email");
        Optional<User> userOptional = userRepository.findByEmail(email);



        userOptional.orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(oauthUser.getAttribute("name"));
            newUser.setActivated(true);

            // 2. Preverimo, ali se email ujema z administratorskim
            if (adminEmail.equalsIgnoreCase(email)) {
                // Če se, uporabnik postane PREDSTAVNIK
                newUser.setRole(Role.PREDSTAVNIK);
            } else {
                // Sicer postane navaden STANOVALEC
                newUser.setRole(Role.STANOVALEC);
            }

            return userRepository.save(newUser);
        });

        return oauthUser;
    }
}