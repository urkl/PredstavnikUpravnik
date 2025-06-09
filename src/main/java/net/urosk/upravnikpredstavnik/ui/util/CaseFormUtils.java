package net.urosk.upravnikpredstavnik.ui.util;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import net.urosk.upravnikpredstavnik.data.Status;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;

import com.vaadin.flow.component.grid.Grid;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;

public class CaseFormUtils {

    public static HorizontalLayout createAddCaseForm(CaseRepository repo, Map<Status, VerticalLayout> columns, Function<Case, Card> cardFactory) {
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
            newCase.setStatus(Status.PREDLOG);
            newCase.setCreatedDate(LocalDateTime.now());
            newCase.setLastModifiedDate(LocalDateTime.now());
            repo.save(newCase);

            VerticalLayout column = columns.get(Status.PREDLOG);
            Card card = cardFactory.apply(newCase);
            column.addComponentAtIndex(1, card);

            titleField.clear();
            descriptionField.clear();
        });

        return formLayout;
    }

    public static HorizontalLayout createResidentCaseForm(CaseRepository repo, AuthenticatedUser auth, Grid<Case> grid) {
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
                newCase.setStatus(Status.PREDLOG);
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
