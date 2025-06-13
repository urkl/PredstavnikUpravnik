// FILE: src/main/java/net/urosk/upravnikpredstavnik/ui/views/PublicCasesView.java
package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.format.DateTimeFormatter;

@Route(value = "public-cases")
@PageTitle("Naloge")
@AnonymousAllowed
public class PublicCasesView extends VerticalLayout {

    private final CaseRepository caseRepository;
    private final VerticalLayout caseCardsLayout = new VerticalLayout();
    private final Button loadMoreButton = new Button("Naloži več");

    private int currentPage = 0;
    private static final int PAGE_SIZE = 10;


    public PublicCasesView(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;

        setSizeFull();
        setAlignItems(Alignment.CENTER);

        H3 title = new H3("Pregled zaključenih primerov");

        caseCardsLayout.setSpacing(true);
        caseCardsLayout.setPadding(false);
        caseCardsLayout.setWidth("80%");

        loadMoreButton.addClickListener(e -> loadCases());

        add(title, caseCardsLayout, loadMoreButton);

        loadCases(); // Naložimo prvo stran
    }

    private void loadCases() {
        Pageable pageable = PageRequest.of(currentPage, PAGE_SIZE, Sort.by("creationDate").descending());
        Page<Case> casePage = caseRepository.findAll( pageable);

        if (casePage.hasContent()) {
            casePage.getContent().forEach(c -> caseCardsLayout.add(createCaseCard(c)));
            currentPage++;
        }

        // Skrijemo gumb, če ni več strani za nalaganje
        loadMoreButton.setVisible(!casePage.isLast());
    }

    private Component createCaseCard(Case aCase) {
        H3 title = new H3(aCase.getTitle());
        title.getStyle().set("margin-top", "0");

        Span status = new Span(aCase.getStatus());
        status.getElement().getThemeList().add("badge success");

        Paragraph description = new Paragraph(aCase.getDescription());

        Span date = new Span("Ustvarjeno: " + aCase.getCreatedDate().format(DateTimeFormatter.ofPattern("dd. MM. yyyy")));
        date.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");

        // Gumb za komentarje (ikona oblačka)
        Button commentsButton = new Button(new Icon(VaadinIcon.COMMENT_O));
        commentsButton.addClickListener(e -> {
            // Logika za prikaz komentarjev - odpre se dialog
            // To je povezano z naslednjo zahtevo
        });

        HorizontalLayout header = new HorizontalLayout(title, status);
        header.setAlignItems(FlexComponent.Alignment.BASELINE);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        HorizontalLayout footer = new HorizontalLayout(date, commentsButton);
        footer.setWidthFull();
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Div card = new Div(header, description, footer);
        card.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("padding", "var(--lumo-space-l)");

        return card;
    }
}