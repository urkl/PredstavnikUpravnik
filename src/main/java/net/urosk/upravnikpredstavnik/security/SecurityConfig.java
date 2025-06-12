package net.urosk.upravnikpredstavnik.security;

import com.vaadin.flow.spring.security.VaadinSavedRequestAwareAuthenticationSuccessHandler;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import net.urosk.upravnikpredstavnik.ui.views.LoginView;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
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
        http.authorizeHttpRequests(auth -> auth
                        // Dovolimo dostop do pogostih statičnih virov Spring Boota
                        // To pokrije npr. /css/**, /js/**, /images/**, /webjars/**, /**/favicon.ico
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

                        // Dovolimo dostop do vaših specifičnih javnih poti, ki jih morda PathRequest ne pokriva.
                        // Uporabite direktne string vzorce za Ant-style matching.
                        // Če so vaše slike in teme že v standardnih statičnih mapah (pokritih zgoraj), lahko te vrstice odstranite.
                        .requestMatchers("/images/**").permitAll() // Splošne slike
                        .requestMatchers("/themes/**").permitAll() // Teme
                        .requestMatchers("/images/*.png").permitAll() // Specifične PNG slike (verjetno redundantno)

                        // Ikone iz line-awesome addona
                        .requestMatchers("/line-awesome/**/*.svg").permitAll()

                // Tukaj dodajte vse druge poti, ki morajo biti javno dostopne
                // .requestMatchers("/moja-javna-pot/**").permitAll()
        );
        http.csrf(csrf -> csrf
                .ignoringRequestMatchers("/VAADIN/dynamic/resource/**") // Sprememba: direktni string vzorec
        );
        super.configure(http);


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