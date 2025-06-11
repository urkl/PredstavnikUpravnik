package net.urosk.upravnikpredstavnik.security;

import net.urosk.upravnikpredstavnik.data.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

// SPREMEMBA: Implementiramo OidcUser namesto OAuth2User
public class CustomOidcUser implements OidcUser {

    private final OidcUser oidcUser;
    private final User appUser;

    public CustomOidcUser(OidcUser oidcUser, User appUser) {
        this.oidcUser = oidcUser;
        this.appUser = appUser;
    }

    // --- Metode iz vmesnika OidcUser ---
    @Override
    public Map<String, Object> getClaims() {
        return oidcUser.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUser.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcUser.getIdToken();
    }

    // --- Metode iz vmesnika OAuth2User (ki ga OidcUser razširja) ---
    @Override
    public Map<String, Object> getAttributes() {
        return oidcUser.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = "ROLE_" + appUser.getRole().name();
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getName() {
        // Uporabimo ime iz našega User objekta za konsistentnost
        return appUser.getName();
    }

    // --- Dodatne metode za lažji dostop ---
    public String getEmail() {
        return appUser.getEmail();
    }
}