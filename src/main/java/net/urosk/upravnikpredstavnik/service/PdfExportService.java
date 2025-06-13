// FILE: src/main/java/net/urosk/upravnikpredstavnik/service/PdfExportService.java
package net.urosk.upravnikpredstavnik.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import net.urosk.upravnikpredstavnik.config.AppProcessProperties;
import net.urosk.upravnikpredstavnik.data.entity.Building;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.entity.Comment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PdfExportService {

    private final AppProcessProperties processProperties;
    // Pripravimo si formatirnike, da jih ne ustvarjamo vedno znova
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy 'ob' HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public PdfExportService(AppProcessProperties processProperties) {
        this.processProperties = processProperties;
    }

    public byte[] generateCasePdf(Case aCase) throws IOException, TemplateException {
        // Uporabimo izolirano FreeMarker konfiguracijo, da se izognemo konfliktom
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setClassForTemplateLoading(this.getClass(), "/templates/");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        // Pripravimo podatkovni model za predlogo
        Map<String, Object> model = buildTemplateModel(aCase);

        // Procesiramo predlogo v HTML
        Template template = cfg.getTemplate("case_report.ftl");
        String htmlContent = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

        // Generiramo PDF
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();

            builder.withHtmlContent(htmlContent, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        }
    }

    /**
     * Zgradi podatkovni model in ročno formatira vse datume v nize.
     */
    private Map<String, Object> buildTemplateModel(Case aCase) {
        Map<String, Object> model = new HashMap<>();
        model.put("case", aCase);
        model.put("status", processProperties.getStatuses().get(aCase.getStatus()));
        model.put("buildings", aCase.getBuildings().stream().map(Building::getName).collect(Collectors.joining(", ")));

        // --- ROČNO FORMATIRANJE DATUMOV ---
        if (aCase.getCreatedDate() != null) {
            model.put("createdDateFormatted", aCase.getCreatedDate().format(DATE_TIME_FORMATTER));
        }
        if (aCase.getLastModifiedDate() != null) {
            model.put("lastModifiedDateFormatted", aCase.getLastModifiedDate().format(DATE_TIME_FORMATTER));
        }
        if (aCase.getStartDate() != null) {
            model.put("startDateFormatted", aCase.getStartDate().format(DATE_FORMATTER));
        }
        if (aCase.getEndDate() != null) {
            model.put("endDateFormatted", aCase.getEndDate().format(DATE_FORMATTER));
        }

        // --- ROČNO FORMATIRANJE DATUMOV V KOMENTARJIH ---
        List<Map<String, String>> formattedComments = aCase.getComments().stream()
                .map(comment -> {
                    Map<String, String> cMap = new HashMap<>();
                    if (comment.getAuthor() != null) {
                        cMap.put("author", comment.getAuthor().getName());
                    }
                    cMap.put("content", comment.getContent());
                    if (comment.getTimestamp() != null) {
                        cMap.put("timestamp", comment.getTimestamp().format(DATE_TIME_FORMATTER));
                    }
                    return cMap;
                })
                .collect(Collectors.toList());
        model.put("comments", formattedComments);

        return model;
    }
}