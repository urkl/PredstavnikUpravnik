package net.urosk.upravnikpredstavnik.security;

import com.vaadin.flow.spring.security.VaadinSavedRequestAwareAuthenticationSuccessHandler;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import net.urosk.upravnikpredstavnik.ui.views.LoginView;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;


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
        // Najprej dovolimo dostop do javnih virov
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/images/**", "/themes/**").permitAll());
        http.authorizeHttpRequests(
                authorize -> authorize.requestMatchers(new AntPathRequestMatcher("/images/*.png")).permitAll());

        // Icons from the line-awesome addon
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/line-awesome/**/*.svg")).permitAll());

        super.configure(http);
        // ----------------------------------------------------------------

        // Sedaj definiramo naše lastne nastavitve, ki bodo imele prednost
        setLoginView(http, LoginView.class, "/logout");

        http.oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(new VaadinSavedRequestAwareAuthenticationSuccessHandler())
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(this.customOAuth2UserService)
                        )
                )
                .rememberMe(rememberMe -> rememberMe
                        .userDetailsService(this.userDetailsService)
                        .key("neka-zelo-dolga-in-varna-skrivna-vrednost") // Uporabi varno vrednost!
                        .tokenValiditySeconds(86400 * 14)
                        .alwaysRemember(true)
                );
    }
}