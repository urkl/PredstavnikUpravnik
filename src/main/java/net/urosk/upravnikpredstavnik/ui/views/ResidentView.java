// FILE: src/main/java/net/urosk/upravnikpredstavnik/ui/views/ResidentView.java
package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
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
import net.urosk.upravnikpredstavnik.data.entity.Building;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.BuildingRepository;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Moje Zadeve")
@PermitAll
public class ResidentView extends VerticalLayout {
    private final CaseRepository caseRepository;
    private final AuthenticatedUser authenticatedUser;
    private final AppProcessProperties appProcessProperties;
    private final BuildingRepository buildingRepository;

    private final VerticalLayout casesLayout;
    private final Optional<User> maybeUser;

    public ResidentView(CaseRepository caseRepository, AuthenticatedUser authenticatedUser, AppProcessProperties appProcessProperties, BuildingRepository buildingRepository) {
        this.caseRepository = caseRepository;
        this.authenticatedUser = authenticatedUser;
        this.appProcessProperties = appProcessProperties;
        this.buildingRepository = buildingRepository;
        this.maybeUser = this.authenticatedUser.get();

        setAlignItems(Alignment.CENTER);
        setPadding(true);
        addClassName("resident-view");

        Div contentWrapper = new Div();
        contentWrapper.setMaxWidth("800px");
        contentWrapper.setWidthFull();

        casesLayout = new VerticalLayout();
        casesLayout.setPadding(false);
        casesLayout.setSpacing(true);
        casesLayout.addClassName("cases-container");

        var h2=new H2("Moje zadnje zadeve");
        h2.addClassName("title");
        contentWrapper.add(createCaseCreationForm(), h2, casesLayout);
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

        MultiSelectComboBox<Building> buildingSelect = new MultiSelectComboBox<>("Izberi objekte");
        buildingSelect.setItems(buildingRepository.findAll());
        buildingSelect.setItemLabelGenerator(Building::getName);
        buildingSelect.setPlaceholder("Izberite enega ali več objektov");
        buildingSelect.setWidthFull();

        FormLayout formLayout = new FormLayout(titleField, descriptionField, buildingSelect);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        binder.forField(titleField).asRequired("Naslov je obvezen.").bind(Case::getTitle, Case::setTitle);
        binder.forField(descriptionField).asRequired("Opis je obvezen.").bind(Case::getDescription, Case::setDescription);
        // SPREMENJENO: Poveži MultiSelectComboBox s Set<Building>
        binder.forField(buildingSelect)
//                .asRequired("Izbira objekta je obvezna.")
                .bind(
                        Case::getBuildings,
                        (caseObject, selectedBuildings) -> caseObject.setBuildings(new HashSet<>(selectedBuildings)) // Pretvori Collection v HashSet
                );

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
            try {
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
                    buildingSelect.clear();
                    refreshCasesList();
                } else {
                    Notification.show("Prosimo, izpolnite vsa obvezna polja in pravilno izberite objekte.", 3000, Notification.Position.MIDDLE);
                }
            } catch (Exception e) {
                Notification.show("Napaka pri shranjevanju: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });

        formWrapper.add(formTitle, formLayout, new Span("Priloge (slike, dokumenti):"), upload, saveButton);
        return formWrapper;
    }

    private void refreshCasesList() {
        casesLayout.removeAll();
        maybeUser.ifPresent(user -> {
            List<Case> cases;

                cases = caseRepository.findFirst10ByAuthorIdOrderByCreatedDateDesc(user.getId());

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

        Div content = new Div();
        content.addClassNames(LumoUtility.Margin.Vertical.MEDIUM);
        content.setText(caseItem.getDescription());
        card.add(content);

        if (caseItem.getBuildings() != null && !caseItem.getBuildings().isEmpty()) {
            HorizontalLayout buildingsLayout = new HorizontalLayout();
            buildingsLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            buildingsLayout.setSpacing(true);
            buildingsLayout.addClassNames(LumoUtility.Margin.Top.XSMALL, LumoUtility.Margin.Bottom.XSMALL, LumoUtility.Padding.Horizontal.SMALL);

            Icon buildingIcon = VaadinIcon.HOME_O.create();
            buildingIcon.setColor("var(--lumo-contrast-50pct)");
            buildingIcon.setSize("16px");

            String buildingNames = caseItem.getBuildings().stream()
                    .map(Building::getName)
                    .collect(Collectors.joining(", "));
            Span buildingsSpan = new Span(buildingNames);
            buildingsSpan.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);

            buildingsLayout.add(buildingIcon, buildingsSpan);
            card.add(buildingsLayout);
        }

        VerticalLayout attachmentsLayout = createAttachmentsLayout(caseItem);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d. M.yyyy 'ob' HH:mm");
        Span timeInfo = new Span("Oddano: " + caseItem.getCreatedDate().format(formatter));
        timeInfo.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.TERTIARY);

        Div actions = new Div();
        boolean canDelete = maybeUser.isPresent() &&
                (caseItem.getAuthor().getId().equals(maybeUser.get().getId()) ||
                        maybeUser.get().getRoles().contains("ROLE_ADMINISTRATOR") ||
                        maybeUser.get().getRoles().contains("ROLE_UPRAVNIK") ||
                        maybeUser.get().getRoles().contains("ROLE_PREDSTAVNIK"));

        if ("PREDLOG".equals(caseItem.getStatus()) && canDelete) {
            Button deleteBtn = new Button("Izbriši", new Icon(VaadinIcon.TRASH));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
            deleteBtn.addClickListener(e -> showDeleteConfirmation(caseItem));
            actions.add(deleteBtn);
        }

        HorizontalLayout footer = new HorizontalLayout(timeInfo, actions);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        footer.setWidthFull();

        card.add(header, attachmentsLayout, footer);
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