package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import net.urosk.upravnikpredstavnik.config.AppProcessProperties;
import net.urosk.upravnikpredstavnik.data.entity.AttachedFile;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Moje Zadeve")
@PermitAll
public class ResidentView extends VerticalLayout {
    private final CaseRepository caseRepository;
    private final AuthenticatedUser authenticatedUser;
    private final AppProcessProperties appProcessProperties;

    private final VerticalLayout casesLayout;
    private final Optional<User> maybeUser;

    public ResidentView(CaseRepository caseRepository, AuthenticatedUser authenticatedUser, AppProcessProperties appProcessProperties) {
        this.caseRepository = caseRepository;
        this.authenticatedUser = authenticatedUser;
        this.appProcessProperties = appProcessProperties;
        this.maybeUser = this.authenticatedUser.get();

        // Glavni layout je centriran
        setAlignItems(Alignment.CENTER);
        setPadding(true);
        addClassName("resident-view");

        // Vsebinski ovoj z omejeno širino
        Div contentWrapper = new Div();
        contentWrapper.setMaxWidth("800px");
        contentWrapper.setWidthFull();

        casesLayout = new VerticalLayout();
        casesLayout.setPadding(false);
        casesLayout.setSpacing(true);
        casesLayout.addClassName("cases-container");

        contentWrapper.add(createCaseCreationForm(), new H2("Moje zadnje zadeve"), casesLayout);
        add(contentWrapper);

        refreshCasesList();
    }

    private Component createCaseCreationForm() {
        VerticalLayout formWrapper = new VerticalLayout();
        formWrapper.getStyle()
                .set("background-color", "rgba(255, 255, 255, 0.95)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-m)")
                .set("padding", "var(--lumo-space-l)");

        H3 formTitle = new H3("Vnesi novo zadevo");

        Binder<Case> binder = new Binder<>(Case.class);
        TextField titleField = new TextField("Naslov zadeve");
        TextArea descriptionField = new TextArea("Opis");

        FormLayout formLayout = new FormLayout(titleField, descriptionField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        binder.forField(titleField).asRequired("Naslov je obvezen.").bind(Case::getTitle, Case::setTitle);
        binder.forField(descriptionField).asRequired("Opis je obvezen.").bind(Case::getDescription, Case::setDescription);

        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/*", ".pdf", ".doc", ".docx", ".xls", ".xlsx");
        upload.getElement().setAttribute("capture", "environment");

        Button saveButton = new Button("Dodaj zadevo", new Icon(VaadinIcon.PLUS_CIRCLE_O));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle().set("margin-top", "var(--lumo-space-m)");

        saveButton.addClickListener(click -> {
            if (maybeUser.isEmpty()) {
                Notification.show("Niste prijavljeni.", 3000, Notification.Position.MIDDLE);
                return;
            }

            Case newCase = new Case();
            if (binder.writeBeanIfValid(newCase)) {
                newCase.setAuthor(maybeUser.get());
                newCase.setStatus(appProcessProperties.getDefaultStatus());
                newCase.setCreatedDate(LocalDateTime.now());
                newCase.setLastModifiedDate(LocalDateTime.now());

                List<AttachedFile> attachedFiles = new ArrayList<>();
                buffer.getFiles().forEach(fileName -> {
                    try (InputStream inputStream = buffer.getInputStream(fileName)) {
                        byte[] content = inputStream.readAllBytes();
                        String mimeType = buffer.getFileData(fileName).getMimeType();
                        attachedFiles.add(new AttachedFile(fileName, mimeType, content));
                    } catch (IOException e) {
                        Notification.show("Napaka pri branju datoteke: " + fileName, 3000, Notification.Position.MIDDLE);
                    }
                });
                newCase.setAttachedFiles(attachedFiles);

                caseRepository.save(newCase);
                Notification.show("Zadeva uspešno dodana!", 2000, Notification.Position.TOP_CENTER);
                binder.readBean(new Case());
                upload.clearFileList();
                refreshCasesList();
            } else {
                Notification.show("Prosimo, izpolnite vsa obvezna polja.", 3000, Notification.Position.MIDDLE);
            }
        });

        formWrapper.add(formTitle, formLayout, new Span("Priloge (slike, dokumenti):"), upload, saveButton);
        return formWrapper;
    }

    private void refreshCasesList() {
        casesLayout.removeAll();
        maybeUser.ifPresent(user -> {
            List<Case> cases = caseRepository.findFirst10ByAuthorIdOrderByCreatedDateDesc(user.getId());
            if (cases.isEmpty()) {
                casesLayout.add(new Span("Nimate še nobene odprte zadeve."));
            } else {
                cases.forEach(caseItem -> casesLayout.add(createResidentCaseCard(caseItem)));
            }
        });
    }

    private Component createResidentCaseCard(Case caseItem) {
        com.vaadin.flow.component.card.Card card = new com.vaadin.flow.component.card.Card();
        card.setWidthFull();
        card.addClassName("resident-case-card");

        // Glava kartice
        H3 title = new H3(caseItem.getTitle());
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        Span status = new Span(appProcessProperties.getStatuses().get(caseItem.getStatus()));
        status.getElement().getThemeList().add("badge");
        if ("PREDLOG".equals(caseItem.getStatus())) status.getElement().getThemeList().add("contrast");
        else if ("ZAKLJUCENO".equals(caseItem.getStatus())) status.getElement().getThemeList().add("success");
        else if ("V_DELU".equals(caseItem.getStatus())) status.getElement().getThemeList().add("primary");

        HorizontalLayout header = new HorizontalLayout(title, status);
        header.setAlignItems(FlexComponent.Alignment.BASELINE);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        // Vsebina kartice
        Div content = new Div();
        content.addClassNames(LumoUtility.Margin.Vertical.MEDIUM);
        content.setText(caseItem.getDescription());

        // Priloge
        VerticalLayout attachmentsLayout = createAttachmentsLayout(caseItem);

        // Noga kartice
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d. M. yyyy 'ob' HH:mm");
        Span timeInfo = new Span("Oddano: " + caseItem.getCreatedDate().format(formatter));
        timeInfo.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.TERTIARY);

        Div actions = new Div();
        if ("PREDLOG".equals(caseItem.getStatus())) {
            Button deleteBtn = new Button("Izbriši", new Icon(VaadinIcon.TRASH));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
            deleteBtn.addClickListener(e -> showDeleteConfirmation(caseItem));
            actions.add(deleteBtn);
        }

        HorizontalLayout footer = new HorizontalLayout(timeInfo, actions);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        footer.setWidthFull();

        card.add(header, content, attachmentsLayout, footer);
        return card;
    }

    private VerticalLayout createAttachmentsLayout(Case caseItem) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);

        List<AttachedFile> files = caseItem.getAttachedFiles();
        if (files != null && !files.isEmpty()) {
            Span title = new Span("Priloge:");
            title.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.XSMALL);
            layout.add(title);

            files.forEach(file -> {
                StreamResource resource = new StreamResource(file.getFileName(), () -> new ByteArrayInputStream(file.getContent()));
                Anchor downloadLink = new Anchor(resource, file.getFileName());
                downloadLink.getElement().setAttribute("download", true);

                Icon fileIcon = new Icon(VaadinIcon.FILE_TEXT_O);
                if (file.getMimeType() != null && file.getMimeType().startsWith("image")) {
                    fileIcon = new Icon(VaadinIcon.FILE_PICTURE);
                }

                HorizontalLayout fileRow = new HorizontalLayout(fileIcon, downloadLink);
                fileRow.setAlignItems(FlexComponent.Alignment.CENTER);
                fileRow.setSpacing(true);
                layout.add(fileRow);
            });
        }
        return layout;
    }

    private void showDeleteConfirmation(Case caseToDelete) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Potrditev brisanja");
        dialog.setText("Ali res želite trajno izbrisati zadevo '" + caseToDelete.getTitle() + "'?");
        dialog.setConfirmText("Izbriši");
        dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
        dialog.addConfirmListener(event -> {
            caseRepository.delete(caseToDelete);
            Notification.show("Zadeva izbrisana.");
            refreshCasesList();
        });
        dialog.setCancelable(true);
        dialog.setCancelText("Prekliči");
        dialog.open();
    }
}
