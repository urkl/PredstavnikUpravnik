// KONČNA VERZIJA PO VZORU DELUJOČEGA PRIMERA
package net.urosk.upravnikpredstavnik.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import net.urosk.upravnikpredstavnik.ui.views.LoginView;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    private final CustomAuthenticationSuccessHandler successHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Dovoli dostop do statičnih virov
        http.authorizeHttpRequests(auth -> auth.requestMatchers(
                "/images/**",
                "/themes/**"
        ).permitAll());

        super.configure(http);

        // Uporabimo nov pristop s 'successHandler'
        http.oauth2Login(oauth2 -> oauth2
                .successHandler(this.successHandler)
        );

        setLoginView(http, LoginView.class, "/logout");
    }
}