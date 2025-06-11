package net.urosk.upravnikpredstavnik.ui.util;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;

public class CaseFormUtils {

    // SPREMEMBA: Mapa sedaj uporablja String kot ključ
    public static HorizontalLayout createAddCaseForm(CaseRepository repo, Map<String, VerticalLayout> columns, String defaultStatus, Function<Case, Card> cardFactory) {
        TextField titleField = new TextField("Naslov");
        TextField descriptionField = new TextField("Opis");
        Button addButton = new Button("Dodaj zadevo", new Icon(VaadinIcon.PLUS));

        titleField.setWidthFull();
        descriptionField.setWidthFull();
        addButton.getStyle().set("margin-top", "auto");

        HorizontalLayout formLayout = new HorizontalLayout(titleField, descriptionField, addButton);
        formLayout.setSpacing(true);
        formLayout.setPadding(true);
        formLayout.setWidthFull();
        formLayout.setFlexGrow(1.0, titleField, descriptionField);
        formLayout.getStyle()
                .set("background-color", "#f8f8f8")
                .set("border-radius", "8px")
                .set("flex-wrap", "wrap");

        addButton.addClickListener(e -> {
            String title = titleField.getValue();
            String desc = descriptionField.getValue();
            if (title == null || title.trim().isEmpty()) return;

            Case newCase = new Case();
            newCase.setTitle(title);
            newCase.setDescription(desc);
            newCase.setStatus(defaultStatus); // Uporabimo privzeti status
            newCase.setCreatedDate(LocalDateTime.now());
            newCase.setLastModifiedDate(LocalDateTime.now());
            repo.save(newCase);

            VerticalLayout column = columns.get(defaultStatus); // Dobimo stolpec preko niza
            Card card = cardFactory.apply(newCase);
            column.addComponentAtIndex(1, card);

            titleField.clear();
            descriptionField.clear();
        });

        return formLayout;
    }

    // SPREMEMBA: Metodi dodamo privzeti status
    public static HorizontalLayout createResidentCaseForm(CaseRepository repo, AuthenticatedUser auth, Grid<Case> grid, String defaultStatus) {
        TextField titleField = new TextField("Naslov");
        TextField descriptionField = new TextField("Opis");
        Button addButton = new Button("Dodaj zadevo", new Icon(VaadinIcon.PLUS));

        titleField.setWidthFull();
        descriptionField.setWidthFull();

        HorizontalLayout form = new HorizontalLayout(titleField, descriptionField, addButton);
        form.setSpacing(true);
        form.setPadding(true);
        form.setWidthFull();
        form.setFlexGrow(1.0, titleField, descriptionField);
        form.getStyle().set("flex-wrap", "wrap");

        addButton.addClickListener(e -> {
            if (auth.get().isPresent()) {
                String title = titleField.getValue();
                String desc = descriptionField.getValue();
                if (title == null || title.trim().isEmpty()) return;

                Case newCase = new Case();
                newCase.setTitle(title);
                newCase.setDescription(desc);
                newCase.setStatus(defaultStatus); // Uporabimo privzeti status
                newCase.setCreatedDate(LocalDateTime.now());
                newCase.setLastModifiedDate(LocalDateTime.now());
                newCase.setAuthor(auth.get().get());


                repo.save(newCase);
                grid.setItems(repo.findByAuthorId(auth.get().get().getId()));

                titleField.clear();
                descriptionField.clear();
            }
        });

        return form;
    }
}