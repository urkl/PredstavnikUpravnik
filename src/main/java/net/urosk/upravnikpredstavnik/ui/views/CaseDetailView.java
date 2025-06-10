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
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler; // <-- PRAVILEN UVOZ
import jakarta.annotation.security.PermitAll;
import net.urosk.upravnikpredstavnik.data.Status;
import net.urosk.upravnikpredstavnik.data.entity.AttachedFile;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;

import java.io.ByteArrayOutputStream; // <-- NOV UVOZ
import java.io.IOException;
import java.io.OutputStream; // <-- NOV UVOZ
import java.util.Optional;

@Route(value = "zadeva", layout = MainLayout.class)
@PageTitle("Podrobnosti Zadeve")
@PermitAll
public class CaseDetailView extends VerticalLayout implements HasUrlParameter<String> {

    private final CaseRepository caseRepository;
    private Case currentCase;

    private final Binder<Case> binder = new Binder<>(Case.class);
    private final TextField title = new TextField("Naslov");
    private final TextArea description = new TextArea("Opis");
    private final RadioButtonGroup<Status> status = new RadioButtonGroup<>("Status");
    private final Grid<AttachedFile> filesGrid = new Grid<>(AttachedFile.class);
    private final Upload upload = new Upload();

    private ByteArrayOutputStream uploadStream;

    public CaseDetailView(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
        setSizeFull();
        status.setItems(Status.values());
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

    private void configureUploader() {
        // Uporabimo UploadHandler, kot si predlagal.
        upload.setReceiver( (fileName, mimeType) -> {
            // Ustvarimo nov tok za vsako datoteko, ki se nalaga.
            uploadStream = new ByteArrayOutputStream();

            // Vrnemo OutputStream, v katerega bo komponenta Upload pisala podatke.
            return uploadStream;
        });
        upload.setAcceptedFileTypes("image/*", ".pdf", ".doc", ".docx");
        upload.setMaxFiles(5);

        upload.addSucceededListener(event -> {
            byte[] content = uploadStream.toByteArray();
            AttachedFile newFile = new AttachedFile(event.getFileName(), event.getMIMEType(), content);
            promptForFileComment(newFile);
        });

        upload.addFailedListener(event -> uploadStream = null);
        upload.addFileRejectedListener(event -> uploadStream = null);
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
        filesGrid.setColumns("fileName",  "comment");
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

            } else {
                UI.getCurrent().navigate(ManagerKanbanView.class);
            }
        }
    }
}