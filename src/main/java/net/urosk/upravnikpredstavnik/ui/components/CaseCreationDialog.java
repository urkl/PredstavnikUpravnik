package net.urosk.upravnikpredstavnik.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import net.urosk.upravnikpredstavnik.config.AppProcessProperties;
import net.urosk.upravnikpredstavnik.data.entity.Building;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.repository.BuildingRepository;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;
import net.urosk.upravnikpredstavnik.service.AuditService;

import java.time.LocalDateTime;
import java.util.HashSet;

public class CaseCreationDialog extends Dialog {

    private final Runnable onSave;
    private final Binder<Case> binder = new Binder<>(Case.class);

    public CaseCreationDialog(CaseRepository caseRepository, AuthenticatedUser authenticatedUser, AppProcessProperties appProcessProperties, BuildingRepository buildingRepository, AuditService auditService, Runnable onSave) {
        this.onSave = onSave;

        setHeaderTitle("Vnos nove zadeve");
        setWidth("600px");

        TextField titleField = new TextField("Naslov zadeve");
        TextArea descriptionField = new TextArea("Opis");
        MultiSelectComboBox<Building> buildingSelect = new MultiSelectComboBox<>("Povezani objekti");

        buildingSelect.setItems(buildingRepository.findAll());
        buildingSelect.setItemLabelGenerator(Building::getName);

        FormLayout formLayout = new FormLayout(titleField, descriptionField, buildingSelect);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        binder.forField(titleField).asRequired("Naslov je obvezen.").bind(Case::getTitle, Case::setTitle);
        binder.forField(descriptionField).asRequired("Opis je obvezen.").bind(Case::getDescription, Case::setDescription);
        binder.forField(buildingSelect).bind(Case::getBuildings, (c, b) -> c.setBuildings(new HashSet<>(b)));

        add(new VerticalLayout(formLayout));

        Button saveButton = new Button("Shrani", new Icon(VaadinIcon.CHECK), e -> saveCase(caseRepository, authenticatedUser, appProcessProperties));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelButton = new Button("Prekliči", e -> close());
        getFooter().add(new HorizontalLayout(saveButton, cancelButton));
    }

    private void saveCase(CaseRepository caseRepository, AuthenticatedUser authenticatedUser, AppProcessProperties appProcessProperties) {
        authenticatedUser.get().ifPresent(user -> {
            Case newCase = new Case();
            if (binder.writeBeanIfValid(newCase)) {
                newCase.setAuthor(user);
                newCase.setStatus(appProcessProperties.getDefaultStatus());
                newCase.setCreatedDate(LocalDateTime.now());
                caseRepository.save(newCase);
                Notification.show("Zadeva uspešno ustvarjena!", 2000, Notification.Position.TOP_CENTER);
                onSave.run();
                close();
            } else {
                Notification.show("Prosimo, izpolnite vsa obvezna polja.", 3000, Notification.Position.MIDDLE);
            }
        });
    }
}