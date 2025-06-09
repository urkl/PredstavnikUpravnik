// FINALNA VERZIJA: src/main/java/net/urosk/upravnikpredstavnik/security/SecurityConfig.java
package net.urosk.upravnikpredstavnik.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import net.urosk.upravnikpredstavnik.ui.views.LoginView;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableWebSecurity
@Configuration
// @EnableMethodSecurity // Ta anotacija ni več potrebna
public class SecurityConfig extends VaadinWebSecurity {

    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers(
                "/images/**",
                "/themes/**"
        ).permitAll());

        super.configure(http);

        http.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(this.customOAuth2UserService)
                )
        );

        setLoginView(http, LoginView.class, "/logout");
    }
}