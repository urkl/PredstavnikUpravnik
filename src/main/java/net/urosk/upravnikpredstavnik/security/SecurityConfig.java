// FILE: src/main/java/net/urosk/upravnikpredstavnik/security/SecurityConfig.java
package net.urosk.upravnikpredstavnik.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import net.urosk.upravnikpredstavnik.ui.views.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers(
                new AntPathRequestMatcher("/images/**"),
                new AntPathRequestMatcher("/themes/**") // Dovoli dostop do teme
        ).permitAll());

        super.configure(http);
        setLoginView(http, LoginView.class, "/logout");
    }
}