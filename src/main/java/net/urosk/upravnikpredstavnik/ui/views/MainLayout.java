package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import net.urosk.upravnikpredstavnik.data.Role;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;

import java.util.Optional;

public class MainLayout extends AppLayout {

    private final AuthenticatedUser authenticatedUser;

    public MainLayout(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
        createTopNavBar();
    }

    private void createTopNavBar() {
        H1 logo = new H1("Upravnik & Predstavnik");
        logo.addClassNames("text-l", "m-m");

        HorizontalLayout navIcons = new HorizontalLayout();
        navIcons.setSpacing(true);

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();

            // Prikaz ikon glede na vlogo
            if (user.getRole() == Role.STANOVALEC) {
                navIcons.add(createIconLink(VaadinIcon.FILE_TEXT, "Moje zadeve", ResidentView.class));
            }

            if (user.getRole() == Role.UPRAVNIK || user.getRole() == Role.PREDSTAVNIK) {
                navIcons.add(
                        createIconLink(VaadinIcon.CALENDAR, "Koledar", CalendarView.class),
                       // createIconLink(VaadinIcon.ARCHIVES, "Poročila", DummyReportsView.class),
                        createIconLink(VaadinIcon.LIST, "Kanban", ManagerKanbanView.class)
                );
            }

            Avatar avatar = new Avatar(user.getName());
            Span userName = new Span(user.getName());
            Button logoutButton = new Button("Odjava", e -> authenticatedUser.logout());

            HorizontalLayout userInfo = new HorizontalLayout(avatar, userName, logoutButton);
            userInfo.setAlignItems(FlexComponent.Alignment.CENTER);

            HorizontalLayout header = new HorizontalLayout(logo, navIcons);
            header.setAlignItems(FlexComponent.Alignment.CENTER);
            header.expand(logo);
            header.setWidthFull();
            header.setPadding(true);
            header.setSpacing(true);

            HorizontalLayout fullBar = new HorizontalLayout(header, userInfo);
            fullBar.setWidthFull();
            fullBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            fullBar.setAlignItems(FlexComponent.Alignment.CENTER);
            addToNavbar(fullBar);
        }
    }

    private RouterLink createIconLink(VaadinIcon icon, String tooltip, Class<? extends Component> navigationTarget) {
        Icon vaadinIcon = icon.create();
        vaadinIcon.getStyle().set("cursor", "pointer");
        vaadinIcon.getElement().setProperty("title", tooltip);

        RouterLink link = new RouterLink( navigationTarget);
        link.add(vaadinIcon);
        link.getElement().getStyle().set("padding", "0.5rem");
        return link;
    }
}
