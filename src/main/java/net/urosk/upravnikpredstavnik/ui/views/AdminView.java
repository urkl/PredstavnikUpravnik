// FILE: src/main/java/net/urosk/upravnikpredstavnik/ui/views/AdminView.java
package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import net.urosk.upravnikpredstavnik.config.AppProcessProperties;
import net.urosk.upravnikpredstavnik.data.entity.Building;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.BuildingRepository;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import net.urosk.upravnikpredstavnik.config.AppSecurityProperties;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;
import net.urosk.upravnikpredstavnik.service.ActiveUserService;
import net.urosk.upravnikpredstavnik.service.AuditService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Administracija")
@PermitAll
public class AdminView extends VerticalLayout {
    private final ActiveUserService activeUserService;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final CaseRepository caseRepository;
    private final AppSecurityProperties appSecurityProperties;
    private final AppProcessProperties appProcessProperties;
    private final AuditService auditService;
    private final AuthenticatedUser authenticatedUser;


    // Layouts for tabs
    private final VerticalLayout userManagementLayout = new VerticalLayout();
    private final VerticalLayout buildingManagementLayout = new VerticalLayout();
    private final VerticalLayout activeUsersLayout = new VerticalLayout();
    private final VerticalLayout deletedCasesLayout = new VerticalLayout();

    // Grids
    private final Grid<User> userGrid = new Grid<>(User.class);
    private final Grid<Building> buildingGrid = new Grid<>(Building.class);
    private final Grid<User> activeUsersGrid = new Grid<>(User.class);
    private final Grid<Case> deletedCasesGrid = new Grid<>(Case.class);

    // Binders
    private final Binder<User> userBinder = new Binder<>(User.class);
    private final Binder<Building> buildingBinder = new Binder<>(Building.class);

    // User management components
    private final MultiSelectComboBox<String> userRolesSelect = new MultiSelectComboBox<>("Vloge uporabnika");
    private final MultiSelectComboBox<Building> userManagedBuildingsSelect = new MultiSelectComboBox<>("Upravljani objekti");
    private final Button saveUserButton = new Button("Shrani vloge in objekte");

    // Building management components
    private final TextField buildingNameField = new TextField("Ime objekta");
    private final TextField buildingAddressField = new TextField("Naslov objekta");
    private final Button saveBuildingButton = new Button("Shrani objekt");
    private final Button deleteBuildingButton = new Button("Izbriši objekt");
    private final Button newBuildingButton = new Button("Nov objekt");

    // Tabs
    private final Tabs mainTabs = new Tabs();


    public AdminView(ActiveUserService activeUserService, UserRepository userRepository, BuildingRepository buildingRepository, CaseRepository caseRepository, AppSecurityProperties appSecurityProperties, AppProcessProperties appProcessProperties, AuditService auditService, AuthenticatedUser authenticatedUser) {
        this.activeUserService = activeUserService;
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
        this.caseRepository = caseRepository;
        this.appSecurityProperties = appSecurityProperties;
        this.appProcessProperties = appProcessProperties;
        this.auditService = auditService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);

        H2 title = new H2("Administracija sistema");
        title.addClassName("title");

        configureTabs();
        configureUserManagement();
        configureBuildingManagement();
        configureActiveUsersManagement();
        configureDeletedCasesManagement(); // <-- DODAN KLIC

        VerticalLayout contentContainer = new VerticalLayout(userManagementLayout, buildingManagementLayout, activeUsersLayout, deletedCasesLayout);
        contentContainer.setPadding(false);
        contentContainer.setSpacing(false);
        contentContainer.setSizeFull();
        contentContainer.addClassName("layout");

        add(title, mainTabs, contentContainer);
        setContentForSelectedTab(mainTabs.getSelectedTab());
    }

    private void configureTabs() {
        Tab userTab = new Tab("Uporabniki");
        Tab buildingTab = new Tab("Objekti");
        Tab activeUsersTab = new Tab("Aktivni uporabniki");
        Tab deletedCasesTab = new Tab("Zbrisane zadeve"); // <-- NOV ZAVIHEK

        mainTabs.add(userTab, buildingTab, activeUsersTab, deletedCasesTab);
        mainTabs.setSelectedTab(userTab);
        mainTabs.addSelectedChangeListener(event -> setContentForSelectedTab(event.getSelectedTab()));
        mainTabs.setWidthFull();
        mainTabs.addClassName("layout");
    }

    private void setContentForSelectedTab(Tab selectedTab) {
        userManagementLayout.setVisible(false);
        buildingManagementLayout.setVisible(false);
        activeUsersLayout.setVisible(false);
        deletedCasesLayout.setVisible(false); // <-- SKRIJEMO NOV LAYOUT

        String selectedLabel = selectedTab.getLabel();
        if ("Uporabniki".equals(selectedLabel)) {
            userManagementLayout.setVisible(true);
            refreshUserGrid();
        } else if ("Objekti".equals(selectedLabel)) {
            buildingManagementLayout.setVisible(true);
            refreshBuildingGrid();
        } else if ("Aktivni uporabniki".equals(selectedLabel)) {
            activeUsersLayout.setVisible(true);
            refreshActiveUsersGrid();
        } else if ("Zbrisane zadeve".equals(selectedLabel)) {
            deletedCasesLayout.setVisible(true); // <-- PRIKAŽEMO NOV LAYOUT
            refreshDeletedCasesGrid();
        }
    }

    private void configureDeletedCasesManagement() {
        deletedCasesLayout.setPadding(false);
        deletedCasesLayout.setSpacing(true);

        H2 sectionTitle = new H2("Zbrisane zadeve");
        deletedCasesGrid.setColumns("title");
        deletedCasesGrid.addColumn(c -> c.getAuthor() != null ? c.getAuthor().getName() : "Neznan avtor")
                .setHeader("Avtor").setSortable(true);
        deletedCasesGrid.addColumn(c -> c.getLastModifiedDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                .setHeader("Datum izbrisa").setSortable(true);
        deletedCasesGrid.addComponentColumn(this::createRestoreButton).setHeader("Dejanja");
        deletedCasesGrid.getColumns().forEach(col -> col.setAutoWidth(true));

        deletedCasesLayout.add(sectionTitle, deletedCasesGrid);
    }

    private Button createRestoreButton(Case caseItem) {
        Button restoreButton = new Button("Povrni", new Icon(VaadinIcon.RECYCLE), click -> {
            ConfirmDialog dialog = new ConfirmDialog(
                    "Potrditev obnove",
                    "Ali res želite obnoviti zadevo '" + caseItem.getTitle() + "'? Status bo postavljen na 'Predlog'.",
                    "Obnovi", e -> {
                caseItem.setStatus(appProcessProperties.getDefaultStatus());
                caseRepository.save(caseItem);
                auditService.log("OBNOVITEV ZADEVE", Case.class, caseItem.getId(), "Zadeva obnovljena", authenticatedUser.get().map(User::getEmail).orElse("SYSTEM"));
                Notification.show("Zadeva obnovljena.", 2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshDeletedCasesGrid();
            },
                    "Prekliči", e -> {}
            );
            dialog.setConfirmButtonTheme(ButtonVariant.LUMO_SUCCESS.getVariantName());
            dialog.open();
        });
        restoreButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY_INLINE);
        return restoreButton;
    }

    private void refreshDeletedCasesGrid() {
        deletedCasesGrid.setItems(caseRepository.findByStatus("DELETED"));
    }

    // ... OSTAJAJOČA KODA IZ PREJŠNJEGA ODGOVORA ...
    private void configureActiveUsersManagement() {
        activeUsersLayout.setPadding(false);
        activeUsersLayout.setSpacing(true);

        H2 sectionTitle = new H2("Trenutno prijavljeni uporabniki");

        Button refreshButton = new Button("Osveži", new Icon(VaadinIcon.REFRESH), e -> refreshActiveUsersGrid());

        activeUsersGrid.setColumns("name", "email");
        activeUsersGrid.addColumn(user -> String.join(", ", user.getRoles()))
                .setHeader("Vloge").setSortable(true);

        activeUsersGrid.getColumns().forEach(col -> col.setAutoWidth(true));

        activeUsersLayout.add(sectionTitle, refreshButton, activeUsersGrid);
    }

    private void refreshActiveUsersGrid() {
        activeUsersGrid.setItems(activeUserService.getActiveUsers());
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