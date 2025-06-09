// KONČNA POPRAVLJENA VERZIJA: src/main/java/net/urosk/upravnikpredstavnik/security/SecurityConfig.java
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

    // 1. Dodamo privatno polje za naš custom servis
    private final CustomOAuth2UserService customOAuth2UserService;

    // 2. Ustvarimo konstruktor, preko katerega bo Spring sam vbrizgal naš servis
    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Popravek zastarelega AntPathRequestMatcher
        http.authorizeHttpRequests(auth -> auth.requestMatchers(
                "/images/**",
                "/themes/**"
        ).permitAll());

        super.configure(http);

        // 3. Uporabimo že vbrizgan servis, namesto da ga iščemo ročno
        http.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(this.customOAuth2UserService)
                )
        );

        setLoginView(http, LoginView.class, "/logout");
    }
}