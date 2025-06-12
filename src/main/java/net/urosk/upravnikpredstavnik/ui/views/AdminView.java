package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import net.urosk.upravnikpredstavnik.data.entity.Building;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.BuildingRepository;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import net.urosk.upravnikpredstavnik.config.AppSecurityProperties;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Administracija")
@PermitAll
public class AdminView extends VerticalLayout {

    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final CaseRepository caseRepository;
    private final AppSecurityProperties appSecurityProperties;

    private final Grid<User> userGrid = new Grid<>(User.class);
    private final MultiSelectComboBox<String> userRolesSelect = new MultiSelectComboBox<>("Vloge uporabnika");
    private final MultiSelectComboBox<Building> userManagedBuildingsSelect = new MultiSelectComboBox<>("Upravljani objekti");
    private final Button saveUserButton = new Button("Shrani vloge in objekte");

    private final Binder<User> userBinder = new Binder<>(User.class);

    private final Grid<Building> buildingGrid = new Grid<>(Building.class);
    private final TextField buildingNameField = new TextField("Ime objekta");
    private final TextField buildingAddressField = new TextField("Naslov objekta");
    private final Button saveBuildingButton = new Button("Shrani objekt");
    private final Button deleteBuildingButton = new Button("Izbriši objekt");
    private final Button newBuildingButton = new Button("Nov objekt");
    private final Binder<Building> buildingBinder = new Binder<>(Building.class);

    private final Tabs mainTabs = new Tabs();
    private final VerticalLayout userManagementLayout = new VerticalLayout();
    private final VerticalLayout buildingManagementLayout = new VerticalLayout();


    public AdminView(UserRepository userRepository, BuildingRepository buildingRepository, CaseRepository caseRepository, AppSecurityProperties appSecurityProperties) {
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
        this.caseRepository = caseRepository;
        this.appSecurityProperties = appSecurityProperties;

        setSizeFull();
        setPadding(true);

        H2 title = new H2("Administracija sistema");

        configureUserManagement();
        configureBuildingManagement();
        configureTabs();

        add(title, mainTabs, userManagementLayout, buildingManagementLayout);

        setContentForSelectedTab(mainTabs.getSelectedTab());
    }

    private void configureTabs() {
        Tab userTab = new Tab("Upravljanje uporabnikov");
        Tab buildingTab = new Tab("Upravljanje objektov");

        mainTabs.add(userTab, buildingTab);
        mainTabs.setSelectedTab(userTab);
        mainTabs.addSelectedChangeListener(event -> setContentForSelectedTab(event.getSelectedTab()));
    }

    private void setContentForSelectedTab(Tab selectedTab) {
        userManagementLayout.setVisible(false);
        buildingManagementLayout.setVisible(false);

        if (selectedTab.getLabel().equals("Upravljanje uporabnikov")) {
            userManagementLayout.setVisible(true);
            refreshUserGrid();
        } else if (selectedTab.getLabel().equals("Upravljanje objektov")) {
            buildingManagementLayout.setVisible(true);
            refreshBuildingGrid();
        }
    }

    private void configureUserManagement() {
        userManagementLayout.setPadding(false);
        userManagementLayout.setSpacing(true);

        H2 userSectionTitle = new H2("Upravljanje vlog uporabnikov");
        userManagementLayout.add(userSectionTitle);


        userGrid.setColumns("name", "email");
        userGrid.addColumn(user -> String.join(", ", user.getRoles()))
                .setHeader("Vloge").setSortable(true);
        userGrid.addColumn(user -> user.getManagedBuildings().stream().map(Building::getName).collect(Collectors.joining(", ")))
                .setHeader("Upravljani objekti").setSortable(true);

        userGrid.getColumns().forEach(col -> col.setAutoWidth(true));
        userGrid.asSingleSelect().addValueChangeListener(event -> editUser(event.getValue()));

        userRolesSelect.setItems(appSecurityProperties.getRoles());
        userRolesSelect.setPlaceholder("Izberi vloge");
        userRolesSelect.setAutoExpand(MultiSelectComboBox.AutoExpandMode.BOTH);

        userManagedBuildingsSelect.setItems(buildingRepository.findAll());
        userManagedBuildingsSelect.setItemLabelGenerator(Building::getName);
        userManagedBuildingsSelect.setPlaceholder("Izberi upravljane objekte");
        userManagedBuildingsSelect.setAutoExpand(MultiSelectComboBox.AutoExpandMode.BOTH);

        FormLayout userFormLayout = new FormLayout();
        userFormLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("300px", 1));
        userFormLayout.getStyle()
                .set("background-color", "rgba(255, 255, 255, 0.95)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 16px rgba(0,0,0,0.2)")
                .set("padding", "1.5rem");
        userFormLayout.add(userRolesSelect, userManagedBuildingsSelect, saveUserButton);

        userBinder.bind(userRolesSelect, User::getRoles, User::setRoles);
        userBinder.bind(userManagedBuildingsSelect, User::getManagedBuildings, User::setManagedBuildings);

        saveUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveUserButton.addClickListener(event -> saveUser());

        userManagementLayout.add(userGrid, userFormLayout);

        userRolesSelect.setVisible(false);
        userManagedBuildingsSelect.setVisible(false);
        saveUserButton.setVisible(false);
    }

    private void editUser(User user) {
        if (user == null) {
            userRolesSelect.setVisible(false);
            userManagedBuildingsSelect.setVisible(false);
            saveUserButton.setVisible(false);
        } else {
            userBinder.setBean(user);
            userRolesSelect.setVisible(true);
            userManagedBuildingsSelect.setVisible(true);
            saveUserButton.setVisible(true);
        }
    }

    private void saveUser() {
        User user = userBinder.getBean();
        if (user != null) {
            try {
                userRepository.save(user);
                Notification.show("Vloge in objekti za uporabnika " + user.getName() + " so shranjeni.", 2000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshUserGrid();
                userGrid.asSingleSelect().clear();
            } catch (Exception e) {
                Notification.show("Napaka pri shranjevanju uporabnika: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }

    private void refreshUserGrid() {
        userGrid.setItems(userRepository.findAll());
        userRolesSelect.clear();
        userManagedBuildingsSelect.clear();
    }

    private void configureBuildingManagement() {
        buildingManagementLayout.setPadding(false);
        buildingManagementLayout.setSpacing(true);
        buildingManagementLayout.setVisible(false);

        H2 buildingSectionTitle = new H2("Upravljanje objektov");
        buildingManagementLayout.add(buildingSectionTitle);

        buildingGrid.setColumns("name", "address");
        buildingGrid.getColumns().forEach(col -> col.setAutoWidth(true));
        buildingGrid.asSingleSelect().addValueChangeListener(event -> editBuilding(event.getValue()));

        FormLayout buildingFormLayout = new FormLayout(buildingNameField, buildingAddressField, saveBuildingButton, deleteBuildingButton, newBuildingButton);
        buildingFormLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("300px", 1));
        buildingFormLayout.getStyle()
                .set("background-color", "rgba(255, 255, 255, 0.95)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 16px rgba(0,0,0,0.2)")
                .set("padding", "1.5rem");

        // ODSTRANJENO: buildingBinder.bindInstanceFields(this);
        // DODANO: Eksplicitno vezanje polj
        buildingBinder.forField(buildingNameField)
                .asRequired("Ime objekta je obvezno.")
                .bind(Building::getName, Building::setName);
        buildingBinder.forField(buildingAddressField)
                .asRequired("Naslov objekta je obvezen.")
                .bind(Building::getAddress, Building::setAddress);


        saveBuildingButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBuildingButton.addClickListener(event -> saveBuilding());

        deleteBuildingButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteBuildingButton.addClickListener(event -> confirmDeleteBuilding());
        deleteBuildingButton.setEnabled(false);

        newBuildingButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        newBuildingButton.addClickListener(event -> newBuilding());

        buildingManagementLayout.add(buildingGrid, buildingFormLayout);

        buildingNameField.setVisible(false);
        buildingAddressField.setVisible(false);
        saveBuildingButton.setVisible(false);
        deleteBuildingButton.setVisible(false);
    }

    private void newBuilding() {
        buildingBinder.setBean(new Building());
        buildingNameField.setVisible(true);
        buildingAddressField.setVisible(true);
        saveBuildingButton.setVisible(true);
        deleteBuildingButton.setEnabled(false);
    }

    private void editBuilding(Building building) {
        if (building == null) {
            buildingNameField.setVisible(false);
            buildingAddressField.setVisible(false);
            saveBuildingButton.setVisible(false);
            deleteBuildingButton.setEnabled(false);
        } else {
            buildingBinder.setBean(building);
            buildingNameField.setVisible(true);
            buildingAddressField.setVisible(true);
            saveBuildingButton.setVisible(true);
            deleteBuildingButton.setEnabled(true);
        }
    }

    private void saveBuilding() {
        Building building = buildingBinder.getBean();
        if (building != null) {
            try {
                // Preverite, ali je ime objekta edinstveno pred shranjevanjem
                if (building.getId() == null && buildingRepository.findByName(building.getName()).isPresent()) {
                    Notification.show("Objekt z imenom '" + building.getName() + "' že obstaja. Izberite drugo ime.", 5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                buildingRepository.save(building);
                Notification.show("Objekt '" + building.getName() + "' shranjen.", 2000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshBuildingGrid();
                buildingGrid.asSingleSelect().clear();
            } catch (Exception e) {
                Notification.show("Napaka pri shranjevanju objekta: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }

    private void confirmDeleteBuilding() {
        Building buildingToDelete = buildingBinder.getBean();
        if (buildingToDelete == null || buildingToDelete.getId() == null) {
            Notification.show("Ni izbranega objekta za brisanje.", 2000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Potrditev brisanja objekta");
        dialog.setText("Ali res želite trajno izbrisati objekt '" + buildingToDelete.getName() + "'?");
        dialog.setConfirmText("Izbriši");
        dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
        dialog.addConfirmListener(event -> deleteBuilding(buildingToDelete));
        dialog.setCancelable(true);
        dialog.setCancelText("Prekliči");
        dialog.open();
    }

    private void deleteBuilding(Building building) {
        List<User> usersManagingBuilding = userRepository.findAll().stream()
                .filter(u -> u.getManagedBuildings() != null && u.getManagedBuildings().contains(building))
                .collect(Collectors.toList());

        List<Case> casesWithBuilding = caseRepository.findAll().stream()
                .filter(c -> c.getBuildings() != null && c.getBuildings().contains(building))
                .collect(Collectors.toList());

        if (!usersManagingBuilding.isEmpty() || !casesWithBuilding.isEmpty()) {
            Notification.show("Objekta ni mogoče izbrisati, ker je še vedno povezan z uporabniki ali zadevami.", 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            buildingRepository.delete(building);
            Notification.show("Objekt '" + building.getName() + "' uspešno izbrisan.", 2000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshBuildingGrid();
            buildingGrid.asSingleSelect().clear();
            buildingNameField.clear();
            buildingAddressField.clear();
            buildingNameField.setVisible(false);
            buildingAddressField.setVisible(false);
            saveBuildingButton.setVisible(false);
            deleteBuildingButton.setEnabled(false);
        } catch (Exception e) {
            Notification.show("Napaka pri brisanju objekta: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void refreshBuildingGrid() {
        buildingGrid.setItems(buildingRepository.findAll());
        buildingBinder.readBean(null);
        deleteBuildingButton.setEnabled(false);
    }
}