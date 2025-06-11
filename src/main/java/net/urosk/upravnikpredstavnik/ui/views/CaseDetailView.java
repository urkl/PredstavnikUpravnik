package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import jakarta.annotation.security.PermitAll;
import net.urosk.upravnikpredstavnik.data.entity.AttachedFile;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.process.AppProcessProperties;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

@Route(value = "zadeva", layout = MainLayout.class)
@PageTitle("Podrobnosti Zadeve")
@PermitAll
public class CaseDetailView extends VerticalLayout implements HasUrlParameter<String> {

    private final CaseRepository caseRepository;
    private final AppProcessProperties appProcessProperties;
    private Case currentCase;

    private final Binder<Case> binder = new Binder<>(Case.class);
    private final TextField title = new TextField("Naslov");
    private final TextArea description = new TextArea("Opis");
    // SPREMEMBA: Uporabimo ComboBox<String> namesto RadioButtonGroup<Status>
    private final ComboBox<String> status = new ComboBox<>("Status");
    private final Grid<AttachedFile> filesGrid = new Grid<>(AttachedFile.class);
    private final Upload upload = new Upload();

    private ByteArrayOutputStream uploadStream;

    public CaseDetailView(CaseRepository caseRepository, AppProcessProperties appProcessProperties) {
        this.caseRepository = caseRepository;
        this.appProcessProperties = appProcessProperties;
        setSizeFull();
        getStyle()
                .set("background-color", "rgba(255, 255, 255, 0.95)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 16px rgba(0,0,0,0.2)")
                .set("padding", "1.5rem");

        setMaxWidth("900px");
        getStyle().set("margin", "0 auto");

        H2 header = new H2("Urejanje zadeve");
        FormLayout formLayout = new FormLayout(title, description, status);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        configureUploader();
        configureFilesGrid();

        Button saveButton = new Button("Shrani", e -> saveCase());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button backButton = new Button("Nazaj na Kanban", e -> UI.getCurrent().navigate(ManagerKanbanView.class));
        HorizontalLayout buttons = new HorizontalLayout(saveButton, backButton);
        buttons.getStyle().set("margin-top", "1rem");

        add(header, formLayout, new H2("Datoteke"), upload, filesGrid, buttons);

        binder.bindInstanceFields(this);
    }

    /**
     * Posodobljena metoda za konfiguracijo nalaganja, ki uporablja UploadHandler.
     */
    private void configureUploader() {
        // Ustvarimo handler, ki bo obdelal vsako naloženo datoteko.
        UploadHandler handler = new UploadHandler() {
            @Override
            public void handleUploadRequest(UploadEvent event) {
                try {
                    // Preberemo vsebino datoteke v polje bajtov
                    byte[] content = event.getInputStream().readAllBytes();
                    AttachedFile newFile = new AttachedFile(event.getFileName(), event.getContentType(), content);

                    // Ker se ta koda lahko izvaja v ločeni niti, moramo za posodobitev
                    // UI-ja uporabiti ui.access()
                    getUI().ifPresent(ui -> ui.access(() -> {
                        promptForFileComment(newFile);
                    }));

                } catch (IOException e) {
                    Notification.show("Napaka pri obdelavi datoteke: " + e.getMessage());
                }
            }
        };

        upload.setUploadHandler(handler);
        upload.setAcceptedFileTypes("image/*", ".pdf", ".doc", ".docx");
        upload.setMaxFiles(5);
    }
    private void promptForFileComment(AttachedFile file) {
        Dialog dialog = new Dialog();
        TextField commentField = new TextField("Komentar za datoteko: " + file.getFileName());
        commentField.setWidth("300px");
        Button confirmButton = new Button("Dodaj", e -> {
            file.setComment(commentField.getValue());
            currentCase.getAttachedFiles().add(file);
            filesGrid.getDataProvider().refreshAll();
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
            filesGrid.getDataProvider().refreshAll();
        })).setHeader("Dejanja");
    }

    private void saveCase() {
        try {
            if (binder.writeBeanIfValid(currentCase)) {
                caseRepository.save(currentCase);
                Notification.show("Zadeva shranjena.", 2000, Notification.Position.TOP_CENTER);
                UI.getCurrent().navigate(ManagerKanbanView.class);
            }
        } catch (Exception e) {
            Notification.show("Napaka pri shranjevanju: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        if (parameter != null && !parameter.isEmpty()) {
            Optional<Case> caseOpt = caseRepository.findById(parameter);
            if (caseOpt.isPresent()) {
                currentCase = caseOpt.get();
                binder.setBean(currentCase);
                filesGrid.setItems(currentCase.getAttachedFiles());

                // Pravilna nastavitev ComboBox-a
                status.setItems(appProcessProperties.getStatuses().keySet());
                status.setItemLabelGenerator(statusKey -> appProcessProperties.getStatuses().get(statusKey));
                status.setValue(currentCase.getStatus()); // Nastavi trenutno vrednost
            } else {
                UI.getCurrent().navigate(ManagerKanbanView.class);
            }
        }
    }
}