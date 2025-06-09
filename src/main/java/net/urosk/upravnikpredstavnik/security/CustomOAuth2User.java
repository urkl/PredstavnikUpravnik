// FINALNA VERZIJA: src/main/java/net/urosk/upravnikpredstavnik/security/CustomOAuth2User.java
package net.urosk.upravnikpredstavnik.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User;
    private final net.urosk.upravnikpredstavnik.data.entity.User appUser;

    public CustomOAuth2User(OAuth2User oauth2User, net.urosk.upravnikpredstavnik.data.entity.User appUser) {
        this.oauth2User = oauth2User;
        this.appUser = appUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // --- SPREMEMBA TUKAJ: Odstranimo predpono "ROLE_" ---
        // Sedaj ustvarimo vlogo z imenom, ki ga @RolesAllowed pričakuje (npr. "PREDSTAVNIK").
        return Collections.singletonList(new SimpleGrantedAuthority(appUser.getRole().name()));
    }

    @Override
    public String getName() {
        return oauth2User.getAttribute("name");
    }

    public String getEmail() {
        return oauth2User.getAttribute("email");
    }
}