package net.urosk.upravnikpredstavnik.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.menu")
public class AppMenuProperties {
    private List<MenuItemInfo> items;

    @Data
    public static class MenuItemInfo {
        private String view;
        private String icon;
        private String tooltip;
    }
}