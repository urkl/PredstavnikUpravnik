package net.urosk.upravnikpredstavnik.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.info")
public class AppInfoProperties {
    private String name;
    private String tagline;
}