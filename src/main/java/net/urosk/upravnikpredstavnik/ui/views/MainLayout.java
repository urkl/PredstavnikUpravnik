package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import net.urosk.upravnikpredstavnik.config.AppMenuProperties;
import net.urosk.upravnikpredstavnik.config.AppSecurityProperties;
import net.urosk.upravnikpredstavnik.data.entity.User;

import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MainLayout extends AppLayout {

    private final AuthenticatedUser authenticatedUser;
    private final AppMenuProperties menuProperties;
    private final AppSecurityProperties securityProperties;

    public MainLayout(AuthenticatedUser authenticatedUser, AppMenuProperties menuProperties, AppSecurityProperties securityProperties) {
        this.authenticatedUser = authenticatedUser;
        this.menuProperties = menuProperties;
        this.securityProperties = securityProperties;
        addClassName("main-layout-dark-navbar");
        createTopNavBar();
    }

    private void createTopNavBar() {
        H1 logo = new H1("Upravnik & Predstavnik");
        logo.addClassNames("text-l", "m-m");
        logo.getStyle().set("color", "white");

        HorizontalLayout navIcons = new HorizontalLayout();
        navIcons.setSpacing(true);
        navIcons.setAlignItems(FlexComponent.Alignment.CENTER);

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            Set<String> userRoles = user.getRoles();

            menuProperties.getItems().forEach(item -> {
                if (isAccessGranted(item.getView(), userRoles)) {
                    try {
                        VaadinIcon vaadinIcon = VaadinIcon.valueOf(item.getIcon());
                        Class<? extends Component> viewClass = (Class<? extends Component>) Class.forName(item.getView());
                        navIcons.add(createIconLink(vaadinIcon, item.getTooltip(), viewClass));
                    } catch (ClassNotFoundException e) {
                        System.err.println("Meni-item error: Razred ni najden: " + item.getView());
                    }
                }
            });

            Avatar avatar = new Avatar(user.getName());
            avatar.getStyle().set("cursor", "pointer");

            // --- TUKAJ JE POPRAVEK ZA VIDNOST AVATARJA ---
            // Dodamo 2px bel rob, da bo avatar izstopal na temnem ozadju.
            avatar.getStyle().set("border", "2px solid var(--lumo-primary-contrast-color)");
            avatar.getStyle().set("color", "black");
            avatar.getStyle().set("background-color", "white");
            // ---------------------------------------------

            ContextMenu userMenu = new ContextMenu();
            userMenu.setTarget(avatar);
            userMenu.setOpenOnClick(true);
            userMenu.addItem(user.getName(), e -> {});
            userMenu.add(new Hr());
            userMenu.addItem("Odjava", e -> authenticatedUser.logout());

            HorizontalLayout userInfo = new HorizontalLayout(avatar, userMenu);
            userInfo.setAlignItems(FlexComponent.Alignment.CENTER);

            HorizontalLayout header = new HorizontalLayout(logo, navIcons);
            header.setAlignItems(FlexComponent.Alignment.CENTER);
            header.setSpacing(true);

            HorizontalLayout fullBar = new HorizontalLayout(header, userInfo);
            fullBar.setWidthFull();
            fullBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            fullBar.setAlignItems(FlexComponent.Alignment.CENTER);
            fullBar.getStyle().set("padding", "0 1rem").set("height", "var(--lumo-size-xl)");

            addToNavbar(fullBar);
        }
    }

    private boolean isAccessGranted(String viewClassName, Set<String> userRoles) {
        List<String> requiredRoles = securityProperties.getViewAccess().get(viewClassName);
        if (requiredRoles == null || requiredRoles.isEmpty()) {
            return true;
        }
        return !Collections.disjoint(userRoles, requiredRoles);
    }

    private RouterLink createIconLink(VaadinIcon icon, String tooltip, Class<? extends Component> navigationTarget) {
        Icon vaadinIcon = icon.create();
        vaadinIcon.setSize("24px");
        vaadinIcon.getStyle().set("color", "white");
        vaadinIcon.getStyle().set("cursor", "pointer");
        vaadinIcon.getElement().setProperty("title", tooltip);

        RouterLink link = new RouterLink(navigationTarget);
        link.getStyle().set("display", "flex").set("align-items", "center").set("height", "100%");
        link.add(vaadinIcon);
        return link;
    }
}