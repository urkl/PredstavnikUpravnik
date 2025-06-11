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
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Uporabnik z e-pošto '" + username + "' ne obstaja."));

        // --- SPREMEMBA: Poenotimo logiko z CustomOidcUser ---
        // Ker imajo vloge v bazi že predpono "ROLE_", jih samo preslikamo.
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        // ----------------------------------------------------

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                "", // Geslo ni v uporabi
                user.isActivated(),
                true,
                true,
                true,
                authorities
        );
    }
}