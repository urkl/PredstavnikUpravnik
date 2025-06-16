// FILE: src/main/java/net/urosk/upravnikpredstavnik/ui/components/CaseFormDialog.java
package net.urosk.upravnikpredstavnik.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.server.StreamResource;
import net.urosk.upravnikpredstavnik.config.AppProcessProperties;
import net.urosk.upravnikpredstavnik.data.entity.*;
import net.urosk.upravnikpredstavnik.data.repository.BuildingRepository;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;
import net.urosk.upravnikpredstavnik.service.AuditService;
import net.urosk.upravnikpredstavnik.service.PdfExportService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

public class CaseFormDialog extends Dialog {

    private final CaseRepository caseRepository;
    private final AuthenticatedUser authenticatedUser;
    private final AuditService auditService;
    private final PdfExportService pdfExportService;
    private final Runnable onSaveCallback;
    private final Case currentCase;

    private final Binder<Case> binder = new Binder<>(Case.class);
    private final MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
    private final Upload upload = new Upload(buffer);
    private final Grid<AttachedFile> filesGrid = new Grid<>();
    private final VerticalLayout subtasksLayout = new VerticalLayout();

    public CaseFormDialog(Case aCase, CaseRepository caseRepository, BuildingRepository buildingRepository, UserRepository userRepository, AppProcessProperties processProperties, AuthenticatedUser authenticatedUser, AuditService auditService, PdfExportService pdfExportService, Runnable onSaveCallback) {
        this.currentCase = aCase;
        this.caseRepository = caseRepository;
        this.authenticatedUser = authenticatedUser;
        this.auditService = auditService;
        this.pdfExportService = pdfExportService;
        this.onSaveCallback = onSaveCallback;

        boolean isNew = aCase.getId() == null;
        setHeaderTitle(isNew ? "Vnos nove zadeve" : "Urejanje zadeve: " + aCase.getTitle());
        setWidth("80vw");
        setMaxWidth("900px");
        setHeight("90vh");

        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);

        VerticalLayout detailsTabContent = createDetailsTabContent(processProperties, buildingRepository, userRepository);
        VerticalLayout subtasksTabContent = createSubtasksTabContent();
        VerticalLayout filesTabContent = createFilesTabContent();

        Tab detailsTab = new Tab("Podrobnosti");
        Tab subtasksTab = new Tab("Podnaloge");
        Tab filesTab = new Tab("Datoteke");
        Tabs tabs = new Tabs(detailsTab, subtasksTab, filesTab);
        Div pages = new Div(detailsTabContent, subtasksTabContent, filesTabContent);
        pages.setSizeFull();
        pages.getStyle().set("padding-top", "var(--lumo-space-m)");

        subtasksTabContent.setVisible(false);
        filesTabContent.setVisible(false);
        tabs.addSelectedChangeListener(event -> {
            detailsTabContent.setVisible(event.getSelectedTab() == detailsTab);
            subtasksTabContent.setVisible(event.getSelectedTab() == subtasksTab);
            filesTabContent.setVisible(event.getSelectedTab() == filesTab);
        });
        mainLayout.expand(pages);

        mainLayout.add(tabs, pages);
        add(mainLayout);

        createFooter(isNew);

        if (isNew) {
            currentCase.setStatus(processProperties.getDefaultStatus());
        }
        binder.readBean(currentCase);
    }

    private VerticalLayout createDetailsTabContent(AppProcessProperties processProperties, BuildingRepository buildingRepository, UserRepository userRepository) {
        // ... koda ostane nespremenjena ...
        RadioButtonGroup<String> status = new RadioButtonGroup<>("Status");
        RadioButtonGroup<Priority> priority = new RadioButtonGroup<>("Prioriteta");
        TextField title = new TextField("Naslov");
        TextArea description = new TextArea("Opis");
        MultiSelectComboBox<Building> buildingSelect = new MultiSelectComboBox<>("Povezani objekti");
        MultiSelectComboBox<User> coordinators = new MultiSelectComboBox<>("Koordinatorji");
        DatePicker startDatePicker = new DatePicker("Datum začetka");
        DatePicker endDatePicker = new DatePicker("Datum zaključka");

        status.setItems(processProperties.getStatuses().keySet());
        status.setItemLabelGenerator(processProperties.getStatuses()::get);
        priority.setItems(Priority.values());
        priority.setItemLabelGenerator(Priority::getDisplayName);
        buildingSelect.setItems(buildingRepository.findAll());
        buildingSelect.setItemLabelGenerator(Building::getName);
        coordinators.setItems(userRepository.findAll());
        coordinators.setItemLabelGenerator(User::getName);
        startDatePicker.setLocale(new Locale("sl", "SI"));
        endDatePicker.setLocale(new Locale("sl", "SI"));

        binder.forField(title).asRequired().bind(Case::getTitle, Case::setTitle);
        binder.forField(description).asRequired().bind(Case::getDescription, Case::setDescription);
        binder.forField(status).asRequired().bind(Case::getStatus, Case::setStatus);
        binder.forField(priority).bind(Case::getPriority, Case::setPriority);
        binder.forField(buildingSelect).bind(c -> new HashSet<>(c.getBuildings()), (c, b) -> c.setBuildings(new HashSet<>(b)));
        binder.forField(coordinators).bind(c -> new HashSet<>(c.getCoordinators()), (c, u) -> c.setCoordinators(new HashSet<>(u)));
        binder.forField(startDatePicker).bind(c -> c.getStartDate() != null ? c.getStartDate().toLocalDate() : null, (c, d) -> c.setStartDate(d != null ? d.atStartOfDay() : null));
        binder.forField(endDatePicker).bind(c -> c.getEndDate() != null ? c.getEndDate().toLocalDate() : null, (c, d) -> c.setEndDate(d != null ? d.atTime(23, 59) : null));

        FormLayout formLayout = new FormLayout(title, description, buildingSelect, coordinators, startDatePicker, endDatePicker, status, priority);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.setColspan(status, 2);
        formLayout.setColspan(priority, 2);

        return new VerticalLayout(formLayout);
    }

    private VerticalLayout createSubtasksTabContent() {
        subtasksLayout.setPadding(false);
        subtasksLayout.setSpacing(true);
        subtasksLayout.setSizeFull();
        refreshSubtasks();
        return subtasksLayout;
    }

    private void refreshSubtasks() {
        subtasksLayout.removeAll();
        ProgressBar progressBar = new ProgressBar();
        progressBar.setWidthFull();
        updateProgressBar(progressBar, currentCase);
        if (currentCase.getSubtasks() != null && !currentCase.getSubtasks().isEmpty()) {
            subtasksLayout.add(progressBar);
        }
        VerticalLayout checkboxLayout = new VerticalLayout();
        checkboxLayout.setPadding(false);
        checkboxLayout.setSpacing(false);
        if (currentCase.getSubtasks() != null) {
            currentCase.getSubtasks().forEach(subtask -> checkboxLayout.add(createSubtaskRow(subtask, progressBar)));
        }
        HorizontalLayout addControls = createAddSubtaskControls(checkboxLayout, progressBar);
        subtasksLayout.add(checkboxLayout, addControls);
    }

    // --- SPREMEMBA: Metoda sedaj ustvari celotno vrstico z gumbom za brisanje ---
    private Component createSubtaskRow(Subtask subtask, ProgressBar progressBar) {
        Checkbox checkbox = new Checkbox(subtask.getTask(), subtask.isCompleted());
        checkbox.addValueChangeListener(event -> {
            subtask.setCompleted(event.getValue());
            updateProgressBar(progressBar, currentCase);
        });

        Button deleteButton = new Button(new Icon(VaadinIcon.TRASH), e -> confirmDeleteSubtask(subtask));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        deleteButton.setTooltipText("Izbriši podnalogo");

        HorizontalLayout subtaskRow = new HorizontalLayout(checkbox, deleteButton);
        subtaskRow.setWidthFull();
        subtaskRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        subtaskRow.setAlignItems(FlexComponent.Alignment.CENTER);
        subtaskRow.expand(checkbox);
        return subtaskRow;
    }

    // --- NOVO: Metoda za potrditev in brisanje podnaloge ---
    private void confirmDeleteSubtask(Subtask subtask) {
        new ConfirmDialog("Potrditev brisanja",
                "Ali res želite izbrisati podnalogo '" + subtask.getTask() + "'? Sprememba bo shranjena s klikom na gumb 'Shrani'.",
                "Izbriši", e -> deleteSubtask(subtask),
                "Prekliči", e -> {}
        ).open();
    }

    private void deleteSubtask(Subtask subtask) {
        if (currentCase.getSubtasks() != null) {
            // SPREMEMBA: Odstranimo samo iz seznama v pomnilniku. Shranjevanje se zgodi ob kliku na glavni gumb "Shrani".
            currentCase.getSubtasks().remove(subtask);
            Notification.show("Podnaloga označena za brisanje.");
            refreshSubtasks(); // Osvežimo samo prikaz v dialogu
        }
    }

    private HorizontalLayout createAddSubtaskControls(VerticalLayout checkboxLayout, ProgressBar progressBar) {
        TextField newSubtaskField = new TextField();
        newSubtaskField.setPlaceholder("Nova podnaloga...");
        Button addButton = new Button("Dodaj", new Icon(VaadinIcon.PLUS), e -> {
            String taskText = newSubtaskField.getValue();
            if (taskText != null && !taskText.trim().isEmpty()) {
                if (currentCase.getSubtasks() == null) currentCase.setSubtasks(new ArrayList<>());
                Subtask newSubtask = new Subtask(taskText, false);
                currentCase.getSubtasks().add(newSubtask);

                checkboxLayout.add(createSubtaskRow(newSubtask, progressBar));
                updateProgressBar(progressBar, currentCase);
                newSubtaskField.clear();
            }
        });
        HorizontalLayout layout = new HorizontalLayout(newSubtaskField, addButton);
        layout.setAlignItems(FlexComponent.Alignment.BASELINE);
        return layout;
    }

    // ... preostanek datoteke ostane enak ...

    private void updateProgressBar(ProgressBar progressBar, Case caseItem) {
        if (caseItem.getSubtasks() == null || caseItem.getSubtasks().isEmpty()) {
            progressBar.setVisible(false);
        } else {
            progressBar.setVisible(true);
            long total = caseItem.getSubtasks().size();
            long completed = caseItem.getSubtasks().stream().filter(Subtask::isCompleted).count();
            progressBar.setValue((double) completed / total);
        }
    }

    private VerticalLayout createFilesTabContent() {
        upload.setAcceptedFileTypes("image/*", ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".txt");
        upload.setMaxFiles(10);
        upload.addSucceededListener(event -> {
            try (InputStream inputStream = buffer.getInputStream(event.getFileName())) {
                byte[] content = inputStream.readAllBytes();
                AttachedFile newFile = new AttachedFile(event.getFileName(), event.getMIMEType(), content);
                promptForFileComment(newFile);
            } catch (IOException e) {
                Notification.show("Napaka pri nalaganju datoteke.");
            }
        });

        configureFilesGrid();
        refreshFilesGrid();

        VerticalLayout filesLayout = new VerticalLayout(upload, filesGrid);
        filesLayout.setSizeFull();
        filesLayout.setPadding(false);
        filesLayout.setSpacing(true);
        filesLayout.expand(filesGrid);
        return filesLayout;
    }

    private void promptForFileComment(AttachedFile file) {
        Dialog dialog = new Dialog();
        TextField commentField = new TextField("Dodaj komentar za datoteko (neobvezno)");
        commentField.setWidth("300px");
        Button confirmButton = new Button("Dodaj datoteko", e -> {
            file.setComment(commentField.getValue());
            if (currentCase.getAttachedFiles() == null) {
                currentCase.setAttachedFiles(new ArrayList<>());
            }
            currentCase.getAttachedFiles().add(file);
            refreshFilesGrid();

            if (currentCase.getId() != null) {
                caseRepository.save(currentCase);
                String userEmail = authenticatedUser.get().map(User::getEmail).orElse("Neznan uporabnik");
                auditService.log("DODANA DATOTEKA", Case.class, currentCase.getId(), file.getFileName(),userEmail);
            }

            dialog.close();
            upload.clearFileList();
        });
        dialog.add(new VerticalLayout(commentField, confirmButton));
        dialog.open();
    }

    private void configureFilesGrid() {
        filesGrid.setWidthFull();
        filesGrid.addColumn(AttachedFile::getFileName).setHeader("Ime datoteke").setFlexGrow(1);
        filesGrid.addColumn(AttachedFile::getComment).setHeader("Komentar").setFlexGrow(1);
        filesGrid.addComponentColumn(file -> {
            Anchor downloadLink = new Anchor(new StreamResource(file.getFileName(), () -> new ByteArrayInputStream(file.getContent())), "");
            downloadLink.getElement().setAttribute("download", true);
            Button downloadButton = new Button(new Icon(VaadinIcon.DOWNLOAD_ALT));
            downloadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            downloadLink.add(downloadButton);
            return downloadLink;
        }).setHeader("Prenesi").setFlexGrow(0).setWidth("100px");
        filesGrid.addComponentColumn(file -> {
            Button deleteButton = new Button(new Icon(VaadinIcon.TRASH), e -> confirmDeleteFile(file));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
            return deleteButton;
        }).setHeader("Izbriši").setFlexGrow(0).setWidth("100px");
    }

    private void refreshFilesGrid() {
        if (currentCase.getAttachedFiles() != null) {
            filesGrid.setItems(currentCase.getAttachedFiles());
        } else {
            filesGrid.setItems(new ArrayList<>());
        }
    }

    private void confirmDeleteFile(AttachedFile file) {
        new ConfirmDialog("Potrditev brisanja", "Ali res želite izbrisati datoteko '" + file.getFileName() + "'?", "Izbriši", e -> {
            currentCase.getAttachedFiles().remove(file);
            refreshFilesGrid();
            if (currentCase.getId() != null) {
                caseRepository.save(currentCase);
                String userEmail = authenticatedUser.get().map(User::getEmail).orElse("Neznan uporabnik");
                auditService.log("ZBRISANA DATOTEKA", Case.class, currentCase.getId(), file.getFileName(),userEmail);
            }
        }, "Prekliči", e -> {}).open();
    }

    private void createFooter(boolean isNew) {
        Button saveButton = new Button("Shrani", new Icon(VaadinIcon.CHECK));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveCase());

        Button cancelButton = new Button("Prekliči");
        cancelButton.addClickListener(e -> close());

        HorizontalLayout footerLayout = new HorizontalLayout(saveButton, cancelButton);
        footerLayout.setSpacing(true);

        if (!isNew) {
            Anchor downloadPdfLink = createPdfDownloadLink();
            footerLayout.add(downloadPdfLink);
        }
        getFooter().add(footerLayout);
    }

    private Anchor createPdfDownloadLink() {
        Button downloadPdfButton = new Button("Izvozi v PDF", new Icon(VaadinIcon.DOWNLOAD_ALT));
        downloadPdfButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        StreamResource pdfResource = new StreamResource("zadeva-" + currentCase.getId() + ".pdf", () -> {
            try {
                if(binder.writeBeanIfValid(currentCase)) {
                    return new ByteArrayInputStream(pdfExportService.generateCasePdf(currentCase));
                }
                return new ByteArrayInputStream(new byte[0]);
            } catch (Exception e) {
                Notification.show("Napaka pri generiranju PDF-ja.");
                e.printStackTrace();
                return new ByteArrayInputStream(new byte[0]);
            }
        });

        Anchor downloadLink = new Anchor();
        downloadLink.setHref(pdfResource);
        downloadLink.getElement().setAttribute("download", true);
        downloadLink.add(downloadPdfButton);
        return downloadLink;
    }

    private void saveCase() {
        if (binder.writeBeanIfValid(currentCase)) {
            boolean isNew = currentCase.getId() == null;
            if (isNew) {
                currentCase.setAuthor(authenticatedUser.get().orElse(null));
                currentCase.setCreatedDate(LocalDateTime.now());
            }
            currentCase.setLastModifiedDate(LocalDateTime.now());
            caseRepository.save(currentCase);
            String userEmail = authenticatedUser.get().map(User::getEmail).orElse("Neznan uporabnik");
            auditService.log(isNew ? "USTVARIL ZADEVO" : "POSODOBIL ZADEVO", Case.class, currentCase.getId(), "Zadeva: " + currentCase.getTitle(),userEmail);
            Notification.show("Zadeva uspešno shranjena.", 2000, Notification.Position.TOP_CENTER);
            onSaveCallback.run();
            close();
        } else {
            Notification.show("Prosimo, izpolnite vsa obvezna polja.");
        }
    }
}