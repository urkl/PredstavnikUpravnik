// FILE: src/main/java/net/urosk/upravnikpredstavnik/ui/views/MainLayout.java
package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import net.urosk.upravnikpredstavnik.config.AppInfoProperties;
import net.urosk.upravnikpredstavnik.config.AppMenuProperties;
import net.urosk.upravnikpredstavnik.config.AppSecurityProperties;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final AuthenticatedUser authenticatedUser;
    private final AppMenuProperties menuProperties;
    private final AppSecurityProperties securityProperties;
    private final AppInfoProperties appInfoProperties;

    private final H1 viewTitle = new H1();

    public MainLayout(AuthenticatedUser authenticatedUser, AppMenuProperties menuProperties, AppSecurityProperties securityProperties, AppInfoProperties appInfoProperties) {
        this.authenticatedUser = authenticatedUser;
        this.menuProperties = menuProperties;
        this.securityProperties = securityProperties;
        this.appInfoProperties = appInfoProperties;

        setPrimarySection(Section.DRAWER);
        setDrawerOpened(false);


        addToNavbar(true, createHeaderContent());
        addToDrawer(createDrawerContent());

        addClassName("main-layout");
    }

    private Component createHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");
        toggle.setTooltipText("Meni");

        viewTitle.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");

        HorizontalLayout header = new HorizontalLayout(toggle, viewTitle);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(viewTitle);
        header.setWidth("100%");
        header.addClassNames("py-0", "px-m");

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            Avatar avatar = new Avatar(user.getName());
            avatar.setClassName("main-layout-avatar");
            avatar.getStyle().set("cursor", "pointer");

            ContextMenu userMenu = new ContextMenu();
            userMenu.setTarget(avatar);
            userMenu.setOpenOnClick(true);
            userMenu.addItem(user.getName(), e -> {});
            userMenu.add(new Hr());
            userMenu.addItem("Odjava", e -> authenticatedUser.logout());

            header.add(avatar);
        }

        return header;
    }

    private Component createDrawerContent() {
        H2 appName = new H2(appInfoProperties.getName());
        appName.addClassNames("text-l", "m-0", "px-s");

        SideNav nav = createNavigation();

        // --- POPRAVEK JE TUKAJ ---
        // Ustvarimo SideNavItem z labelom, potjo in ikono
        SideNavItem calendarNavItem = new SideNavItem("Naroči se na koledar", "/public/calendar.ics", VaadinIcon.CALENDAR_CLOCK.create());
        // Dodamo atribut 'download', da brskalnik sproži prenos datoteke
        calendarNavItem.getElement().setAttribute("download", true);
        // Dodamo ključni atribut 'router-ignore', ki Vaadin usmerjevalniku prepreči obravnavo te povezave
        calendarNavItem.getElement().setAttribute("router-ignore", "");
        // --- KONEC POPRAVKA ---

        VerticalLayout drawerLayout = new VerticalLayout(appName, nav, new Hr(), calendarNavItem);
        drawerLayout.setSizeFull();
        drawerLayout.setPadding(false);
        drawerLayout.setSpacing(false);
        drawerLayout.getThemeList().set("spacing-s", true);
        drawerLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

        return drawerLayout;
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();


        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            Set<String> userRoles = user.getRoles();

            for (AppMenuProperties.MenuItemInfo item : menuProperties.getItems()) {
                if (isAccessGranted(item.getView(), userRoles)) {
                    try {
                        Class<? extends Component> viewClass = (Class<? extends Component>) Class.forName(item.getView());
                        SideNavItem navItem = new SideNavItem(item.getTooltip(), viewClass, VaadinIcon.valueOf(item.getIcon()).create());
                        nav.addItem(navItem);
                    } catch (ClassNotFoundException | IllegalArgumentException e) {
                        System.err.println("Meni-item error: " + e.getMessage());
                    }
                }
            }
        }
        return nav;
    }

    private boolean isAccessGranted(String viewClassName, Set<String> userRoles) {
        List<String> requiredRoles = securityProperties.getViewAccess().get(viewClassName);
        if (requiredRoles == null || requiredRoles.isEmpty()) {
            return true;
        }
        return !Collections.disjoint(userRoles, requiredRoles);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        viewTitle.setText(getCurrentPageTitle(event));

     //   if (getElement().getProperty("overlay", false)) {
            setDrawerOpened(false);
       // }
    }

    private String getCurrentPageTitle(AfterNavigationEvent event) {
        return event.getActiveChain().stream()
                .filter(component -> component.getClass().isAnnotationPresent(PageTitle.class))
                .map(component -> component.getClass().getAnnotation(PageTitle.class).value())
                .findFirst()
                .orElse("");
    }
}