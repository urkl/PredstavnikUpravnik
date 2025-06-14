
package net.urosk.upravnikpredstavnik;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@Theme(value = "default")
@PWA(
        name = "BlokApp",
        shortName = "BlokApp",
        backgroundColor = "#ffffff",
        themeColor = "#1C2833"
)
@EnableAsync // OMOGOČI ASINHRONO IZVAJANJE¸
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}