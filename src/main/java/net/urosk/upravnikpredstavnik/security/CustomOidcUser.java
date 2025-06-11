package net.urosk.upravnikpredstavnik.security;

import net.urosk.upravnikpredstavnik.data.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.Map;

/**
 * Ta razred je "ovitek" okoli standardnega OidcUser objekta.
 * Springu posreduje pravilne vloge iz naše baze podatkov.
 */
public class CustomOidcUser implements OidcUser {

    private final OidcUser oidcUser;
    private final User appUser;

    public CustomOidcUser(OidcUser oidcUser, User appUser) {
        this.oidcUser = oidcUser;
        this.appUser = appUser;
    }

    // --- Metode, ki jih zahteva vmesnik OidcUser ---

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

    @Override
    public Map<String, Object> getAttributes() {
        return oidcUser.getAttributes();
    }

    /**
     * Ključna metoda, ki Springu posreduje vloge uporabnika.
     * Sedaj pravilno obdela seznam vlog.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // --- POPRAVEK: Logika je sedaj bolj čista ---
        // Ker imajo vloge že predpono "ROLE_", jih samo preslikamo v SimpleGrantedAuthority.
        return appUser.getRoles().stream()
                .map(SimpleGrantedAuthority::new) // Nič več ročnega sestavljanja niza
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return appUser.getName();
    }

    public String getEmail() {
        return appUser.getEmail();
    }
}