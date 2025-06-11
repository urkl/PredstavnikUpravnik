// KONČNA POPRAVLJENA VERZIJA: src/main/java/net/urosk/upravnikpredstavnik/security/SecurityConfig.java
package net.urosk.upravnikpredstavnik.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import net.urosk.upravnikpredstavnik.ui.views.LoginView;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;



@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, UserDetailsService userDetailsService) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/images/**", "/themes/**").permitAll());

        super.configure(http);

        http.oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                // SPREMEMBA: Uporabimo .oidcUserService() namesto .userService()
                                .oidcUserService(this.customOAuth2UserService)
                        )
                )
                .rememberMe(rememberMe -> rememberMe
                        .userDetailsService(this.userDetailsService)
                        .key("neka-zelo-dolga-in-varna-skrivna-vrednost")
                        .tokenValiditySeconds(86400 * 14)
                );

        setLoginView(http, LoginView.class, "/logout");
    }
}
