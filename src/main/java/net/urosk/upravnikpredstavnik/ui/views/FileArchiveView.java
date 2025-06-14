package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.PermitAll;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.urosk.upravnikpredstavnik.config.AppProcessProperties;
import net.urosk.upravnikpredstavnik.data.entity.AttachedFile;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.repository.BuildingRepository;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;
import net.urosk.upravnikpredstavnik.service.AuditService;
import net.urosk.upravnikpredstavnik.service.PdfExportService;
import net.urosk.upravnikpredstavnik.ui.components.CaseFormDialog;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "files", layout = MainLayout.class)
@PageTitle("Arhiv Datotek")
@PermitAll
public class FileArchiveView extends VerticalLayout {

    private final Grid<FileWrapper> grid = new Grid<>();
    private final TextField searchField = new TextField();
    private List<FileWrapper> allFiles;

    private final CaseRepository caseRepository;
    private final BuildingRepository buildingRepository;
    private final UserRepository userRepository;
    private final AppProcessProperties processProperties;
    private final AuthenticatedUser authenticatedUser;
    private final AuditService auditService;
    private final PdfExportService pdfExportService;


    public FileArchiveView(CaseRepository caseRepository, BuildingRepository buildingRepository, UserRepository userRepository, AppProcessProperties processProperties, AuthenticatedUser authenticatedUser, AuditService auditService, PdfExportService pdfExportService) {
        this.caseRepository = caseRepository;
        this.buildingRepository = buildingRepository;
        this.userRepository = userRepository;
        this.processProperties = processProperties;
        this.authenticatedUser = authenticatedUser;
        this.auditService = auditService;
        this.pdfExportService = pdfExportService;

        addClassName("file-archive-view");
        setSizeFull();

        H2 title = new H2("Arhiv vseh datotek");
        title.addClassName("title");

        configureSearch();
        configureGrid();

        add(title, searchField, grid);

        loadData();
    }

    private void configureSearch() {
        searchField.setPlaceholder("Išči po imenu datoteke ali komentarju...");
        searchField.setWidthFull();
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> filterData(e.getValue()));
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(new ComponentRenderer<>(this::createCaseLink))
                .setHeader("Zadeva").setFlexGrow(1).setSortable(true);
        grid.addColumn(fw -> fw.getAttachedFile().getFileName())
                .setHeader("Ime datoteke").setFlexGrow(1).setSortable(true);
        grid.addColumn(fw -> fw.getAttachedFile().getComment())
                .setHeader("Komentar").setFlexGrow(2).setSortable(true);

        // === POPRAVLJEN KLIC ===
        grid.addColumn(fw -> fw.getACase().getCreatedDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .setHeader("Datum").setFlexGrow(0).setWidth("120px").setSortable(true);
        // =======================

        grid.addComponentColumn(this::createDownloadButton)
                .setHeader("Prenos").setFlexGrow(0).setWidth("100px");
    }

    private Anchor createCaseLink(FileWrapper fileWrapper) {
        // === POPRAVLJEN KLIC ===
        Button caseButton = new Button(fileWrapper.getACase().getTitle());
        caseButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        caseButton.addClickListener(e -> {
            new CaseFormDialog(fileWrapper.getACase(), caseRepository, buildingRepository, userRepository, processProperties, authenticatedUser, auditService, pdfExportService, this::loadData).open();
        });
        // =======================

        // Uporaba Anchorja je tukaj samo zato, da dobimo videz povezave.
        // Klik dejansko sproži zgornji listener, ne navigacije.
        Anchor linkWrapper = new Anchor();
        linkWrapper.add(caseButton);
        return linkWrapper;
    }

    private Anchor createDownloadButton(FileWrapper fileWrapper) {
        AttachedFile file = fileWrapper.getAttachedFile();
        StreamResource resource = new StreamResource(file.getFileName(), () -> new ByteArrayInputStream(file.getContent()));
        Anchor downloadLink = new Anchor(resource, "");
        downloadLink.getElement().setAttribute("download", true);
        Button downloadButton = new Button(new Icon(VaadinIcon.DOWNLOAD_ALT));
        downloadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        downloadLink.add(downloadButton);
        return downloadLink;
    }

    private void loadData() {
        allFiles = new ArrayList<>();
        caseRepository.findAll().forEach(aCase -> {
            if (aCase.getAttachedFiles() != null) {
                aCase.getAttachedFiles().forEach(file -> {
                    allFiles.add(new FileWrapper(aCase, file));
                });
            }
        });
        filterData(searchField.getValue());
    }

    private void filterData(String searchTerm) {
        List<FileWrapper> filesToDisplay = allFiles;
        if (searchTerm != null && !searchTerm.isBlank()) {
            String lowerCaseFilter = searchTerm.toLowerCase();
            filesToDisplay = allFiles.stream()
                    .filter(fw -> {
                        boolean nameMatches = fw.getAttachedFile().getFileName().toLowerCase().contains(lowerCaseFilter);
                        boolean commentMatches = fw.getAttachedFile().getComment() != null && fw.getAttachedFile().getComment().toLowerCase().contains(lowerCaseFilter);
                        return nameMatches || commentMatches;
                    })
                    .collect(Collectors.toList());
        }
        grid.setItems(filesToDisplay);
    }

    @Getter
    @AllArgsConstructor
    private static class FileWrapper {
        private final Case aCase;
        private final AttachedFile attachedFile;
    }
}