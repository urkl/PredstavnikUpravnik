// FILE: src/main/java/net/urosk/upravnikpredstavnik/ui/views/MainLayout.java
package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;
import net.urosk.upravnikpredstavnik.data.Role;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;

import java.util.Optional;

public class MainLayout extends AppLayout {

    private final AuthenticatedUser authenticatedUser;

    public MainLayout(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
        createHeader();
    }

    private void createHeader() {
        H1 logo = new H1("Upravnik & Predstavnik");
        logo.addClassNames("text-l", "m-m");

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            Avatar avatar = new Avatar(user.getName());

            Span userName = new Span(user.getName());
            Button logoutButton = new Button("Odjava", e -> authenticatedUser.logout());

            HorizontalLayout userLayout = new HorizontalLayout(avatar, userName, logoutButton);
            userLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            header.add(userLayout);
        }

        addToNavbar(header);
        createDrawer(maybeUser);
    }

    private void createDrawer(Optional<User> maybeUser) {
        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);

        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            if (user.getRole() == Role.STANOVALEC) {
                tabs.add(createTab("Moje zadeve", ResidentView.class));
            }
            if (user.getRole() == Role.UPRAVNIK || user.getRole() == Role.PREDSTAVNIK) {
                tabs.add(createTab("Kanban pregled", ManagerKanbanView.class));
                // Tukaj dodaj linke za druge poglede (npr. koledar, poročila)
            }
        }
        addToDrawer(tabs);
    }

    private Tab createTab(String viewName, Class<? extends Component> navigationTarget) {
        RouterLink link = new RouterLink();
        link.add(viewName);
        link.setRoute(navigationTarget);
        link.setTabIndex(-1);
        return new Tab(link);
    }
}