package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import jakarta.annotation.security.PermitAll;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

// Uvozi za Flexmark
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.data.MutableDataSet;

import com.vladsch.flexmark.ext.emoji.EmojiExtension; // Za emojije
import java.util.Arrays; // NOV UVOZ

@Route(value = "pomoc", layout = MainLayout.class)
@PageTitle("Pomoč")
@PermitAll
public class HelpView extends VerticalLayout {

    public HelpView() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setPadding(true);
        setMaxWidth("800px");
        getStyle().set("margin", "0 auto");

        H2 header = new H2();
        Span headerSpan = new Span("Pomoč in navodila");
        headerSpan.addClassName("title-badge");
        header.add(headerSpan);
        add(header);

        Div contentContainer = new Div();
        contentContainer.addClassName("layout");
        contentContainer.setWidthFull();

        String markdownContent;
        try {
            ClassPathResource resource = new ClassPathResource("static/help.md");
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] bdata = FileCopyUtils.copyToByteArray(inputStream);
                markdownContent = new String(bdata, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            markdownContent = "Napaka pri nalaganju pomoči: " + e.getMessage() + "\nProsimo, preverite prisotnost datoteke help.md v src/main/resources/static/";
            e.printStackTrace();
        }

        // Prikaz vsebine Markdowna s Flexmark-all
        Div markdownDiv = new Div();
        markdownDiv.getElement().setProperty("innerHTML", convertMarkdownToHtml(markdownContent));

        contentContainer.add(markdownDiv);

        add(contentContainer);
    }

    // Posodobljena metoda za pretvorbo Markdowna v HTML z uporabo Flexmark
    private String convertMarkdownToHtml(String markdown) {
        MutableDataSet options = new MutableDataSet();

        // Uporabite GitHub Flavored Markdown (GFM) profil za boljšo združljivost
        options.setFrom(ParserEmulationProfile.GITHUB_DOC);

        // Omogočite razširitve za tabele in emojije
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                EmojiExtension.create()
        ));

        // Nastavite pot za emojije (če uporabljate :smile: sintakso)
        // Flexmark privzeto uporablja GitHub pot, kar je običajno ok.
        // options.set(EmojiExtension.ROOT_IMAGE_PATH, "/images/emoji/"); // Primer, če imate svoje slike emojijev

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // Pretvori Markdown v HTML
        return renderer.render(parser.parse(markdown));
    }
}