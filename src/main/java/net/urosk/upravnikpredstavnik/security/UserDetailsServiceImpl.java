// LOKACIJA NOVE DATOTEKE: src/main/java/net/urosk/upravnikpredstavnik/security/UserDetailsServiceImpl.java
package net.urosk.upravnikpredstavnik.security;

import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Poiščemo uporabnika v bazi po e-pošti (ki je naše uporabniško ime)
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Uporabnik z e-pošto '" + username + "' ne obstaja."));

        // SPREMEMBA: Ustvarimo avtoriteto za vsako vlogo na seznamu
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        // Vrnemo standardni Spring Security UserDetails objekt
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                "", // Geslo ni v uporabi pri OAuth2, pustimo prazno
                user.isActivated(),
                true,
                true,
                true,
                authorities
        );
    }
}