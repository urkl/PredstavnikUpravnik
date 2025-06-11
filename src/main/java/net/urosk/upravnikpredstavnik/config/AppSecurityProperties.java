package net.urosk.upravnikpredstavnik.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {
    private List<String> allowedMimeTypes;
    private String defaultRole;
    private String rememberMeToken;
    private List<String> roles;
    private Map<String, List<String>> viewAccess;
}