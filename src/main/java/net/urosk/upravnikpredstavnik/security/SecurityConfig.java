// ZAMENJAJ CELOTNO DATOTEKO S TO VSEBINO
package net.urosk.upravnikpredstavnik.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import net.urosk.upravnikpredstavnik.ui.views.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    private final CustomOAuth2UserService customOAuth2UserService;

    // Ali ta konstruktor obstaja?
    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Dovoli dostop do statičnih virov
        http.authorizeHttpRequests(auth -> auth.requestMatchers(
                "/images/**",
                "/themes/**"
        ).permitAll());



        // --- TUKAJ JE KLJUČNI DEL, KI JE MANJKAL ---
        // S tem Springu povemo, naj za OAuth2 prijavo uporabi naš servis po meri.
        http.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(this.customOAuth2UserService)
                )
        );
        // ------------------------------------------------

        setLoginView(http, LoginView.class, "/logout");

        super.configure(http);
    }
}