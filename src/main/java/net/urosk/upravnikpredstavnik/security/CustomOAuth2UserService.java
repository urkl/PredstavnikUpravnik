// PREDLAGANA POSODOBITEV: src/main/java/net/urosk/upravnikpredstavnik/security/CustomOAuth2UserService.java
package net.urosk.upravnikpredstavnik.security;

import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
// SPREMEMBA: Razširimo OidcUserService namesto DefaultOAuth2UserService
public class CustomOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;
    private final AppSecurityProperties appSecurityProperties;
    @Value("${app.admin-email}")
    private String adminEmail;

    public CustomOAuth2UserService(UserRepository userRepository, AppSecurityProperties appSecurityProperties) {
        this.userRepository = userRepository;
        this.appSecurityProperties = appSecurityProperties;
    }

    @Override
    // SPREMEMBA: Metoda sedaj sprejema in vrača OidcUser objekte
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Najprej pokličemo originalno metodo, da dobimo OIDC podatke
        OidcUser oidcUser = super.loadUser(userRequest);
        String email = oidcUser.getAttribute("email");

        // Logika za iskanje in shranjevanje uporabnika ostaja popolnoma enaka
        User appUser = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    System.out.println("!!! Uporabnik z emailom " + email + " ne obstaja. Ustvarjam novega... !!!");
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(oidcUser.getAttribute("name"));
                    newUser.setActivated(true);

                    if (adminEmail.equalsIgnoreCase(email)) {
                        // Admin je lahko predstavnik IN upravnik
                        newUser.setRoles(Set.of("ROLE_ADMINISTRATOR", "ROLE_PREDSTAVNIK", "ROLE_UPRAVNIK", "ROLE_STANOVALEC"));

                    } else {
                        // Navaden uporabnik dobi samo privzeto vlogo
                        newUser.setRoles(Set.of(appSecurityProperties.getDefaultRole()));
                    }

                    User savedUser = userRepository.save(newUser);
                    System.out.println("!!! USPEŠNO SHRANJEN NOV UPORABNIK Z ID-JEM: " + savedUser.getId() + " !!!");
                    return savedUser;
                });

        // Vrnemo nov, prilagojen objekt, ki vsebuje tako OIDC podatke kot našega uporabnika iz baze
        return new CustomOidcUser(oidcUser, appUser);
    }
}