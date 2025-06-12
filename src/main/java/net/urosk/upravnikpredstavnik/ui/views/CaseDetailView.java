package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import net.urosk.upravnikpredstavnik.config.AppProcessProperties;
import net.urosk.upravnikpredstavnik.config.AppSecurityProperties;
import net.urosk.upravnikpredstavnik.data.entity.AttachedFile;
import net.urosk.upravnikpredstavnik.data.entity.Building;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.entity.Subtask;
import net.urosk.upravnikpredstavnik.data.repository.BuildingRepository;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Route(value = "zadeva", layout = MainLayout.class)
@PageTitle("Podrobnosti Zadeve")
@PermitAll
public class CaseDetailView extends VerticalLayout implements HasUrlParameter<String> {

    private final CaseRepository caseRepository;
    private final AppProcessProperties appProcessProperties;
    private final AppSecurityProperties appSecurityProperties;
    private final BuildingRepository buildingRepository;
    private Case currentCase;

    private final Binder<Case> binder = new Binder<>(Case.class);
    private final TextField title = new TextField("Naslov");
    private final TextArea description = new TextArea("Opis");
    private final ComboBox<String> status = new ComboBox<>("Status");
    private final MultiSelectComboBox<Building> buildingSelect = new MultiSelectComboBox<>("Izberi objekte");
    private final DatePicker startDatePicker = new DatePicker("Datum začetka");
    private final DatePicker endDatePicker = new DatePicker("Datum zaključka");

    private final Grid<AttachedFile> filesGrid = new Grid<>(AttachedFile.class);
    private final Upload upload = new Upload();

    private final VerticalLayout subtaskComponent = new VerticalLayout();

    public CaseDetailView(CaseRepository caseRepository, AppProcessProperties appProcessProperties, AppSecurityProperties appSecurityProperties, BuildingRepository buildingRepository) {
        this.caseRepository = caseRepository;
        this.appProcessProperties = appProcessProperties;
        this.appSecurityProperties = appSecurityProperties;
        this.buildingRepository = buildingRepository;

        setMaxWidth("900px");
        getStyle().set("margin", "0 auto");
        getStyle().set("background-color", "rgba(255, 255, 255, 0.95)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 16px rgba(0,0,0,0.2)")
                .set("padding", "1.5rem");

        H2 header = new H2("Urejanje zadeve");
        FormLayout formLayout = new FormLayout(title, description, status, buildingSelect, startDatePicker, endDatePicker);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        configureUploader();
        configureFilesGrid();
        configureBuildingSelect();
        configureDatePickers();

        subtaskComponent.setPadding(false);
        subtaskComponent.setSpacing(false);

        Button saveButton = new Button("Shrani", e -> saveCase());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button backButton = new Button("Nazaj na Kanban", e -> UI.getCurrent().navigate(ManagerKanbanView.class));
        HorizontalLayout buttons = new HorizontalLayout(saveButton, backButton);
        buttons.getStyle().set("margin-top", "1rem");

        add(header, formLayout, new H2("Podnaloge"), subtaskComponent, new H2("Datoteke"), upload, filesGrid, buttons);

        binder.bindInstanceFields(this);
    }

    private void configureUploader() {
        UploadHandler handler = event -> {
            try (InputStream inputStream = event.getInputStream()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                inputStream.transferTo(baos);
                byte[] content = baos.toByteArray();

                AttachedFile newFile = new AttachedFile(event.getFileName(), event.getContentType(), content);

                getUI().ifPresent(ui -> ui.access(() -> promptForFileComment(newFile)));

            } catch (IOException e) {
                getUI().ifPresent(ui -> ui.access(() ->
                        Notification.show("Napaka pri obdelavi datoteke: " + e.getMessage())
                ));
            }
        };

        upload.setUploadHandler(handler);
        upload.setAcceptedFileTypes(appSecurityProperties.getAllowedMimeTypes().toArray(new String[0]));
        upload.setMaxFiles(110);
    }

    private void promptForFileComment(AttachedFile file) {
        Dialog dialog = new Dialog();
        TextField commentField = new TextField("Komentar za datoteko: " + file.getFileName());
        commentField.setWidth("300px");
        Button confirmButton = new Button("Dodaj", e -> {
            file.setComment(commentField.getValue());
            if (currentCase.getAttachedFiles() == null) {
                currentCase.setAttachedFiles(new ArrayList<>());
            }
            currentCase.getAttachedFiles().add(file);
            filesGrid.setItems(currentCase.getAttachedFiles());
            dialog.close();
            Notification.show("Datoteka dodana.");
        });
        dialog.add(new VerticalLayout(commentField, confirmButton));
        dialog.open();
    }

    private void configureFilesGrid() {
        filesGrid.setColumns("fileName", "comment");
        filesGrid.addComponentColumn(file -> new Button("Izbriši", e -> {
            currentCase.getAttachedFiles().remove(file);
            filesGrid.setItems(currentCase.getAttachedFiles());
        })).setHeader("Dejanja");
    }

    private void configureBuildingSelect() {
        buildingSelect.setItems(buildingRepository.findAll());
        buildingSelect.setItemLabelGenerator(Building::getName);
        binder.forField(buildingSelect)
                .bind(
                        Case::getBuildings,
                        (caseObject, selectedBuildings) -> caseObject.setBuildings(new HashSet<>(selectedBuildings))
                );
    }

    private void configureDatePickers() {
        startDatePicker.setLocale(new Locale("sl", "SI"));
        endDatePicker.setLocale(new Locale("sl", "SI"));

        binder.forField(startDatePicker)
                .withConverter(
                        // POPRAVEK: Obravnavanje null vrednosti pri pretvorbi LocalDate v LocalDateTime
                        localDate -> localDate == null ? null : localDate.atStartOfDay(),
                        // Obravnavanje null vrednosti pri pretvorbi LocalDateTime v LocalDate
                        localDateTime -> localDateTime == null ? null : localDateTime.toLocalDate()
                )
                .bind(Case::getStartDate, Case::setStartDate);

        binder.forField(endDatePicker)
                .withConverter(
                        // POPRAVEK: Obravnavanje null vrednosti pri pretvorbi LocalDate v LocalDateTime
                        localDate -> localDate == null ? null : localDate.atTime(23, 59, 59),
                        // Obravnavanje null vrednosti pri pretvorbi LocalDateTime v LocalDate
                        localDateTime -> localDateTime == null ? null : localDateTime.toLocalDate()
                )
                .bind(Case::getEndDate, Case::setEndDate);
    }

    private void refreshSubtaskComponent() {
        subtaskComponent.removeAll();

        ProgressBar progressBar = createSubtaskProgressBar(currentCase);
        VerticalLayout checkboxLayout = new VerticalLayout();
        checkboxLayout.setPadding(false);
        checkboxLayout.setSpacing(false);

        if (currentCase.getSubtasks() != null) {
            currentCase.getSubtasks().forEach(subtask ->
                    checkboxLayout.add(createSubtaskCheckbox(subtask, currentCase, progressBar))
            );
        }

        HorizontalLayout addControls = createAddSubtaskControls(currentCase, checkboxLayout, progressBar);

        subtaskComponent.add(progressBar, checkboxLayout, addControls);
    }

    private ProgressBar createSubtaskProgressBar(Case caseItem) {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setWidthFull();
        updateProgressBar(progressBar, caseItem);
        return progressBar;
    }

    private void updateProgressBar(ProgressBar progressBar, Case caseItem) {
        if (caseItem.getSubtasks() == null || caseItem.getSubtasks().isEmpty()) {
            progressBar.setVisible(false);
            return;
        }
        progressBar.setVisible(true);
        long total = caseItem.getSubtasks().size();
        long completed = caseItem.getSubtasks().stream().filter(Subtask::isCompleted).count();
        progressBar.setValue((double) completed / total);
    }

    private Component createSubtaskCheckbox(Subtask subtask, Case parentCase, ProgressBar progressBar) {
        Checkbox checkbox = new Checkbox(subtask.getTask(), subtask.isCompleted());

        Runnable updateStyle = () -> {
            if (checkbox.getValue()) {
                checkbox.getStyle().set("text-decoration", "line-through").set("color", "var(--lumo-secondary-text-color)");
            } else {
                checkbox.getStyle().remove("text-decoration").remove("color");
            }
        };
        updateStyle.run();

        checkbox.addValueChangeListener(event -> {
            subtask.setCompleted(event.getValue());
            caseRepository.save(parentCase);
            updateStyle.run();
            updateProgressBar(progressBar, parentCase);
        });

        Button deleteSubtaskBtn = new Button(new Icon(VaadinIcon.TRASH));
        deleteSubtaskBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        deleteSubtaskBtn.setTooltipText("Izbriši podnalogo");
        deleteSubtaskBtn.addClickListener(e -> showDeleteSubtaskConfirmation(subtask, parentCase));

        HorizontalLayout subtaskRow = new HorizontalLayout(checkbox, deleteSubtaskBtn);
        subtaskRow.setAlignItems(FlexComponent.Alignment.CENTER);
        subtaskRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        subtaskRow.setWidthFull();
        return subtaskRow;
    }

    private void showDeleteSubtaskConfirmation(Subtask subtaskToDelete, Case parentCase) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Potrditev brisanja podnaloge");
        dialog.setText("Ali res želite izbrisati podnalogo '" + subtaskToDelete.getTask() + "'?");
        dialog.setConfirmText("Izbriši");
        dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
        dialog.addConfirmListener(event -> deleteSubtask(subtaskToDelete, parentCase));
        dialog.setCancelable(true);
        dialog.setCancelText("Prekliči");
        dialog.open();
    }

    private void deleteSubtask(Subtask subtaskToDelete, Case parentCase) {
        if (parentCase.getSubtasks() != null) {
            parentCase.getSubtasks().remove(subtaskToDelete);
            caseRepository.save(parentCase);
            Notification.show("Podnaloga izbrisana.", 1000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshSubtaskComponent();
            updateProgressBar(createSubtaskProgressBar(parentCase), parentCase);
        }
    }

    private HorizontalLayout createAddSubtaskControls(Case caseItem, VerticalLayout checkboxLayout, ProgressBar progressBar) {
        Button toggleBtn = new Button("Dodaj podnalogo", new Icon(VaadinIcon.PLUS_CIRCLE));
        toggleBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);

        TextField field = new TextField();
        field.setPlaceholder("Nova podnaloga...");
        field.setWidth("250px");

        Button confirmBtn = new Button("Dodaj", new Icon(VaadinIcon.CHECK));
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        HorizontalLayout addRow = new HorizontalLayout(field, confirmBtn);
        addRow.setVisible(false);
        addRow.setAlignItems(FlexComponent.Alignment.BASELINE);

        toggleBtn.addClickListener(e -> {
            addRow.setVisible(true);
            toggleBtn.setVisible(false);
            field.focus();
        });

        confirmBtn.addClickListener(e -> {
            String taskText = field.getValue();
            if (taskText != null && !taskText.trim().isEmpty()) {
                Subtask newSubtask = new Subtask();
                newSubtask.setTask(taskText);
                newSubtask.setCompleted(false);

                if (caseItem.getSubtasks() == null) caseItem.setSubtasks(new ArrayList<>());
                caseItem.getSubtasks().add(newSubtask);
                caseRepository.save(caseItem);

                checkboxLayout.add(createSubtaskCheckbox(newSubtask, caseItem, progressBar));
                updateProgressBar(progressBar, caseItem);

                field.clear();
                addRow.setVisible(false);
                toggleBtn.setVisible(true);
            }
        });

        return new HorizontalLayout(toggleBtn, addRow);
    }

    private void saveCase() {
        try {
            if (binder.writeBeanIfValid(currentCase)) {
                currentCase.setLastModifiedDate(LocalDateTime.now());
                caseRepository.save(currentCase);
                Notification.show("Zadeva shranjena.", 2000, Notification.Position.TOP_CENTER);
                UI.getCurrent().navigate(ManagerKanbanView.class);
            }
        } catch (Exception e) {
            Notification.show("Napaka pri shranjevanju: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
            if (!binder.isValid()) {
                binder.validate();
                Notification.show("Prosimo, popravite napake v obrazcu.", 3000, Notification.Position.MIDDLE);
            }
        }
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        if (parameter != null && !parameter.isEmpty()) {
            Optional<Case> caseOpt = caseRepository.findById(parameter);
            if (caseOpt.isPresent()) {

                status.setItems(appProcessProperties.getStatuses().keySet());

                currentCase = caseOpt.get();
                binder.setBean(currentCase);


                status.setItemLabelGenerator(statusKey -> appProcessProperties.getStatuses().get(statusKey));
                status.setValue(currentCase.getStatus());

                if (currentCase.getBuildings() != null) {
                    buildingSelect.setValue(new ArrayList<>(currentCase.getBuildings()));
                } else {
                    buildingSelect.clear();
                }

                startDatePicker.setValue(currentCase.getStartDate() != null ? currentCase.getStartDate().toLocalDate() : null);
                endDatePicker.setValue(currentCase.getEndDate() != null ? currentCase.getEndDate().toLocalDate() : null);


                if (currentCase.getAttachedFiles() == null) {
                    currentCase.setAttachedFiles(new ArrayList<>());
                }
                filesGrid.setItems(currentCase.getAttachedFiles());

                refreshSubtaskComponent();
            } else {
                UI.getCurrent().navigate(ManagerKanbanView.class);
            }
        }
    }
}