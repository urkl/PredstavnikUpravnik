package net.urosk.upravnikpredstavnik.process;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "app.process")
public class AppProcessProperties {
    private Map<String, String> statuses;
    private String defaultStatus;
}