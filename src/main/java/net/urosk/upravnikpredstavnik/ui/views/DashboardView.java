package net.urosk.upravnikpredstavnik.ui.views;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.*;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.legend.Position;
import com.github.appreciated.apexcharts.helper.Series;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import net.urosk.upravnikpredstavnik.config.AppProcessProperties;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.entity.Priority;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("Nadzorna Plošča")
@PermitAll
public class DashboardView extends VerticalLayout {

    private final CaseRepository caseRepository;
    private final AppProcessProperties processProperties;

    public DashboardView(CaseRepository caseRepository, AppProcessProperties processProperties) {
        this.caseRepository = caseRepository;
        this.processProperties = processProperties;

        addClassName("dashboard-view");
        setAlignItems(Alignment.CENTER);
        setPadding(true);

        H2 title = new H2("Pregled Stanja");
        title.addClassName("title");
        add(title);

        List<Case> allCases = caseRepository.findAll();

        // Layout za statistične kartice
        HorizontalLayout statsLayout = createStatsLayout(allCases);
        statsLayout.setWidth("80%");
        statsLayout.getStyle().set("flex-wrap", "wrap").set("justify-content", "center");

        // Layout za grafe
        HorizontalLayout chartsLayout = new HorizontalLayout(createStatusChart(allCases), createPriorityChart(allCases));
        chartsLayout.setWidth("90%");
        chartsLayout.getStyle().set("flex-wrap", "wrap").set("justify-content", "center");

        add(statsLayout, chartsLayout);
    }

    private VerticalLayout createStatCard(String label, String value) {
        Span valueSpan = new Span(value);
        valueSpan.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD);
        Span labelSpan = new Span(label);
        labelSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        VerticalLayout card = new VerticalLayout(valueSpan, labelSpan);
        card.setAlignItems(Alignment.CENTER);
        card.addClassName("layout"); // Uporabimo enak stil kot drugje
        card.getStyle().set("margin", "var(--lumo-space-s)");
        return card;
    }

    private HorizontalLayout createStatsLayout(List<Case> cases) {
        long openCases = cases.stream().filter(c -> !"ZAKLJUCENO".equals(c.getStatus())).count();
        long highPriority = cases.stream().filter(c -> c.getPriority() == Priority.HIGH).count();

        return new HorizontalLayout(
                createStatCard("Skupaj Zadev", String.valueOf(cases.size())),
                createStatCard("Odprtih", String.valueOf(openCases)),
                createStatCard("Visoka Prioriteta", String.valueOf(highPriority))
        );
    }

    private ApexCharts createStatusChart(List<Case> cases) {
        Map<String, Long> statusCounts = cases.stream()
                .collect(Collectors.groupingBy(c -> processProperties.getStatuses().get(c.getStatus()), Collectors.counting()));

        return ApexChartsBuilder.get()
                .withChart(ChartBuilder.get().withType(Type.DONUT).build())
                .withTitle(TitleSubtitleBuilder.get().withText("Zadeve po Statusu").build())
                .withLegend(LegendBuilder.get().withPosition(Position.BOTTOM).build())
                .withSeries(statusCounts.entrySet().stream()
                        .map(e -> new Series<>(e.getKey(), e.getValue().doubleValue()))
                        .toArray(Series[]::new))
                .withLabels(statusCounts.keySet().toArray(new String[0]))
                .build();
    }

    private ApexCharts createPriorityChart(List<Case> cases) {
        Map<Priority, Long> priorityCounts = cases.stream()
                .filter(c -> c.getPriority() != null)
                .collect(Collectors.groupingBy(Case::getPriority, Collectors.counting()));

        return ApexChartsBuilder.get()
                .withChart(ChartBuilder.get().withType(Type.PIE).build())
                .withTitle(TitleSubtitleBuilder.get().withText("Zadeve po Prioriteti").build())
                .withLegend(LegendBuilder.get().withPosition(Position.BOTTOM).build())
                .withColors(priorityCounts.keySet().stream().map(Priority::getColor).toArray(String[]::new))
                .withSeries(priorityCounts.entrySet().stream()
                        .map(e -> new Series<>(e.getKey().getDisplayName(), e.getValue().doubleValue()))
                        .toArray(Series[]::new))
                .withLabels(priorityCounts.keySet().stream().map(Priority::getDisplayName).toArray(String[]::new))
                .build();
    }
}