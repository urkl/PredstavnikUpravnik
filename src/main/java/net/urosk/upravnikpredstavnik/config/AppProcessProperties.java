package net.urosk.upravnikpredstavnik.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "app.process")
public class AppProcessProperties {
    private Map<String, String> statuses;
    private String defaultStatus;
}