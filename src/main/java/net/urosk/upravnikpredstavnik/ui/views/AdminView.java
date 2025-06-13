// FILE: src/main/java/net/urosk/upravnikpredstavnik/ui/views/AdminView.java
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Administracija")
@PermitAll
public class AdminView extends VerticalLayout {

    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final CaseRepository caseRepository;
    private final AppSecurityProperties appSecurityProperties;

    // Komponente za upravljanje uporabnikov
    private final Grid<User> userGrid = new Grid<>(User.class);
    private final MultiSelectComboBox<String> userRolesSelect = new MultiSelectComboBox<>("Vloge uporabnika");
    private final MultiSelectComboBox<Building> userManagedBuildingsSelect = new MultiSelectComboBox<>("Upravljani objekti");
    private final Button saveUserButton = new Button("Shrani vloge in objekte");
    private final Binder<User> userBinder = new Binder<>(User.class);

    // Komponente za upravljanje objektov
    private final Grid<Building> buildingGrid = new Grid<>(Building.class);
    private final TextField buildingNameField = new TextField("Ime objekta");
    private final TextField buildingAddressField = new TextField("Naslov objekta");
    private final Button saveBuildingButton = new Button("Shrani objekt");
    private final Button deleteBuildingButton = new Button("Izbriši objekt");
    private final Button newBuildingButton = new Button("Nov objekt");
    private final Binder<Building> buildingBinder = new Binder<>(Building.class);

    // Zavihki in postavitve
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
        title.addClassName("title");

        // Konfiguracija vsebine
        configureTabs();
        configureUserManagement();
        configureBuildingManagement();

        // Vsebinski kontejner, ki drži postavitve, ki se preklapljajo
        VerticalLayout contentContainer = new VerticalLayout(userManagementLayout, buildingManagementLayout);
        contentContainer.setPadding(false);
        contentContainer.setSpacing(false);
        contentContainer.setSizeFull();
        contentContainer.addClassName("layout");

        // Dodajanje komponent v glavni pogled
        add(title, mainTabs, contentContainer);

        // Nastavitev začetne vsebine glede na izbran zavihek
        setContentForSelectedTab(mainTabs.getSelectedTab());
    }

    private void configureTabs() {
        Tab userTab = new Tab("Upravljanje uporabnikov");
        Tab buildingTab = new Tab("Upravljanje objektov");
        mainTabs.addClassName("layout");
        mainTabs.add(userTab, buildingTab);
        mainTabs.setSelectedTab(userTab);
        mainTabs.addSelectedChangeListener(event -> setContentForSelectedTab(event.getSelectedTab()));
        mainTabs.setWidthFull();
    }

    private void setContentForSelectedTab(Tab selectedTab) {
        // Skrij obe postavitvi
        userManagementLayout.setVisible(false);
        buildingManagementLayout.setVisible(false);

        // Prikaži samo izbrano
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

        H2 userSectionTitle = new H2("Urejanje vlog in objektov uporabnikov");
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

        userManagedBuildingsSelect.setItems(buildingRepository.findAll());
        userManagedBuildingsSelect.setItemLabelGenerator(Building::getName);
        userManagedBuildingsSelect.setPlaceholder("Izberi upravljane objekte");

        FormLayout userFormLayout = new FormLayout(userRolesSelect, userManagedBuildingsSelect, saveUserButton);
        userFormLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("300px", 1));

        userBinder.forField(userRolesSelect).bind(User::getRoles, (user, roles) -> user.setRoles(new HashSet<>(roles)));
        userBinder.forField(userManagedBuildingsSelect).bind(User::getManagedBuildings, (user, buildings) -> user.setManagedBuildings(new HashSet<>(buildings)));

        saveUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveUserButton.addClickListener(event -> saveUser());

        userManagementLayout.add(userGrid, userFormLayout);

        // Formo na začetku skrijemo
        editUser(null);
    }

    private void editUser(User user) {
        userBinder.setBean(user);
        boolean isUserSelected = user != null;

        userRolesSelect.setVisible(isUserSelected);
        userManagedBuildingsSelect.setVisible(isUserSelected);
        saveUserButton.setVisible(isUserSelected);

        if (isUserSelected) {
            userRolesSelect.setValue(new ArrayList<>(user.getRoles()));
            userManagedBuildingsSelect.setValue(new ArrayList<>(user.getManagedBuildings()));
        }
    }

    private void saveUser() {
        User user = userBinder.getBean();
        if (user != null && userBinder.writeBeanIfValid(user)) {
            userRepository.save(user);
            Notification.show("Podatki za uporabnika " + user.getName() + " so shranjeni.", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshUserGrid();
            editUser(null);
        } else {
            Notification.show("Napaka pri shranjevanju.", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void refreshUserGrid() {
        userGrid.setItems(userRepository.findAll());
    }

    private void configureBuildingManagement() {
        buildingManagementLayout.setPadding(false);
        buildingManagementLayout.setSpacing(true);

        H2 buildingSectionTitle = new H2("Urejanje objektov");
        buildingManagementLayout.add(buildingSectionTitle);

        buildingGrid.setColumns("name", "address");
        buildingGrid.getColumns().forEach(col -> col.setAutoWidth(true));
        buildingGrid.asSingleSelect().addValueChangeListener(event -> editBuilding(event.getValue()));

        FormLayout buildingFormLayout = new FormLayout(buildingNameField, buildingAddressField, saveBuildingButton, deleteBuildingButton, newBuildingButton);
        buildingFormLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("300px", 1));

        buildingBinder.forField(buildingNameField).asRequired("Ime objekta je obvezno.").bind(Building::getName, Building::setName);
        buildingBinder.forField(buildingAddressField).asRequired("Naslov objekta je obvezen.").bind(Building::getAddress, Building::setAddress);

        saveBuildingButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBuildingButton.addClickListener(event -> saveBuilding());

        deleteBuildingButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteBuildingButton.addClickListener(event -> confirmDeleteBuilding());

        newBuildingButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        newBuildingButton.addClickListener(event -> newBuilding());

        buildingManagementLayout.add(buildingGrid, buildingFormLayout);

        // Formo na začetku skrijemo
        editBuilding(null);
    }

    private void newBuilding() {
        editBuilding(new Building());
    }

    private void editBuilding(Building building) {
        buildingBinder.setBean(building);
        boolean isBuildingSelected = building != null;

        buildingNameField.setVisible(isBuildingSelected);
        buildingAddressField.setVisible(isBuildingSelected);
        saveBuildingButton.setVisible(isBuildingSelected);
        deleteBuildingButton.setVisible(isBuildingSelected && building.getId() != null);
    }

    private void saveBuilding() {
        Building building = buildingBinder.getBean();
        if (building != null && buildingBinder.writeBeanIfValid(building)) {
            // Preverimo, če objekt s tem imenom že obstaja
            if (building.getId() == null && buildingRepository.findByName(building.getName()).isPresent()) {
                Notification.show("Objekt z imenom '" + building.getName() + "' že obstaja.", 4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            buildingRepository.save(building);
            Notification.show("Objekt '" + building.getName() + "' shranjen.", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshBuildingGrid();
            editBuilding(null);
        }
    }

    private void confirmDeleteBuilding() {
        Building building = buildingBinder.getBean();
        if (building == null || building.getId() == null) return;

        // Preverimo, če je objekt povezan z uporabniki ali zadevami
        boolean isUsedByUser = userRepository.findAll().stream().anyMatch(u -> u.getManagedBuildings().contains(building));
        boolean isUsedInCase = caseRepository.findAll().stream().anyMatch(c -> c.getBuildings().contains(building));

        if (isUsedByUser || isUsedInCase) {
            Notification.show("Objekta ni mogoče izbrisati, ker je v uporabi.", 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog(
                "Izbris objekta",
                "Ali ste prepričani, da želite izbrisati objekt '" + building.getName() + "'?",
                "Izbriši", e -> deleteBuilding(building),
                "Prekliči", e -> {}
        );
        dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
        dialog.open();
    }

    private void deleteBuilding(Building building) {
        buildingRepository.delete(building);
        Notification.show("Objekt '" + building.getName() + "' izbrisan.", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        refreshBuildingGrid();
        editBuilding(null);
    }

    private void refreshBuildingGrid() {
        buildingGrid.setItems(buildingRepository.findAll());
    }
}