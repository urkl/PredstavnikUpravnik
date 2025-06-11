package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import net.urosk.upravnikpredstavnik.config.AppSecurityProperties;

@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Administracija")
@PermitAll
public class AdminView extends VerticalLayout {

    private final UserRepository userRepository;
    private final AppSecurityProperties appSecurityProperties;

    private final Grid<User> userGrid = new Grid<>(User.class);
    private final MultiSelectComboBox<String> rolesSelect = new MultiSelectComboBox<>("Vloge uporabnika");
    private final Button saveButton = new Button("Shrani vloge");

    private final Binder<User> binder = new Binder<>(User.class);

    public AdminView(UserRepository userRepository, AppSecurityProperties appSecurityProperties) {
        this.userRepository = userRepository;
        this.appSecurityProperties = appSecurityProperties;

        H2 title = new H2("Upravljanje vlog uporabnikov");

        configureGrid();
        configureForm();

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("300px",1));
        formLayout        .getStyle()
                .set("background-color", "rgba(255, 255, 255, 0.95)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 16px rgba(0,0,0,0.2)")
                .set("padding", "1.5rem");
        rolesSelect.setAutoExpand(MultiSelectComboBox.AutoExpandMode.BOTH);
        formLayout.add(rolesSelect,1);
        formLayout.add(saveButton,1);
        add(title, userGrid, formLayout);
        refreshGrid();
    }

    private void configureGrid() {
        userGrid.setColumns("name", "email");
        userGrid.addColumn(user -> String.join(", ", user.getRoles()))
                .setHeader("Vloge").setSortable(true);

        userGrid.getColumns().forEach(col -> col.setAutoWidth(true));
        userGrid.asSingleSelect().addValueChangeListener(event -> editUser(event.getValue()));
    }

    private void configureForm() {
        // Napolnimo ComboBox z vsemi možnimi vlogami iz application.yml
        rolesSelect.setItems(appSecurityProperties.getRoles());

        // Povežemo ComboBox z 'roles' poljem v User entiteti
        binder.bind(rolesSelect, User::getRoles, User::setRoles);

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveUser());

        // Na začetku so polja za urejanje skrita
        rolesSelect.setVisible(false);
        saveButton.setVisible(false);
    }

    private void editUser(User user) {
        if (user == null) {
            rolesSelect.setVisible(false);
            saveButton.setVisible(false);
        } else {
            // Povežemo izbranega uporabnika z obrazcem
            binder.setBean(user);
            rolesSelect.setVisible(true);
            saveButton.setVisible(true);
        }
    }

    private void saveUser() {
        User user = binder.getBean();
        if (user != null) {
            userRepository.save(user);
            Notification.show("Vloge za uporabnika " + user.getName() + " so shranjene.", 2000, Notification.Position.MIDDLE);
            refreshGrid();
            // Počistimo izbiro
            userGrid.asSingleSelect().clear();
        }
    }

    private void refreshGrid() {
        userGrid.setItems(userRepository.findAll());
    }
}