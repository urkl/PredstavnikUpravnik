package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import org.vaadin.stefan.fullcalendar.Entry;
import org.vaadin.stefan.fullcalendar.FullCalendar;
import org.vaadin.stefan.fullcalendar.FullCalendarBuilder;
import org.vaadin.stefan.fullcalendar.dataprovider.InMemoryEntryProvider;


import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Route(value = "koledar", layout = MainLayout.class)
@PageTitle("Koledar")
@PermitAll
public class CalendarView extends VerticalLayout {

    private final CaseRepository caseRepository;
    private final InMemoryEntryProvider<Entry> entryProvider = new InMemoryEntryProvider<>();
    private final FullCalendar calendar = FullCalendarBuilder.create().build();
    ;
    private final ComboBox<Integer> yearSelect = new ComboBox<>("Leto");
    private final ComboBox<String> monthSelect = new ComboBox<>("Mesec");

    private static final Map<String, Integer> SLO_MONTHS = Map.ofEntries(
            Map.entry("januar", 1), Map.entry("februar", 2), Map.entry("marec", 3),
            Map.entry("april", 4), Map.entry("maj", 5), Map.entry("junij", 6),
            Map.entry("julij", 7), Map.entry("avgust", 8), Map.entry("september", 9),
            Map.entry("oktober", 10), Map.entry("november", 11), Map.entry("december", 12)
    );

    public CalendarView(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;

        setSizeFull();
        setPadding(true);
        addClassNames("calendar-view", LumoUtility.Padding.MEDIUM);

        configureTopBar();
        configureCalendar();

        add(createTopBar(), calendar);
        loadCasesForSelectedMonth(); // začetno nalaganje
    }

    private void configureTopBar() {
        int currentYear = LocalDate.now().getYear();
        List<Integer> years = IntStream.rangeClosed(currentYear - 5, currentYear + 3)
                .boxed().collect(Collectors.toList());
        yearSelect.setItems(years);
        yearSelect.setValue(currentYear);
        yearSelect.setWidth("120px");

        List<String> months = new ArrayList<>(SLO_MONTHS.keySet());
        monthSelect.setItems(months);
        monthSelect.setValue(LocalDate.now().getMonth().getDisplayName(java.time.format.TextStyle.FULL, new Locale("sl")));
        monthSelect.setWidth("140px");

        yearSelect.addValueChangeListener(e -> loadCasesForSelectedMonth());
        monthSelect.addValueChangeListener(e -> loadCasesForSelectedMonth());
    }

    private HorizontalLayout createTopBar() {
        HorizontalLayout topBar = new HorizontalLayout(monthSelect, yearSelect);
        topBar.setAlignItems(FlexComponent.Alignment.BASELINE);
        topBar.setSpacing(true);
        topBar.setPadding(false);
        return topBar;
    }

    private void configureCalendar() {
    calendar.setSizeFull();
        calendar.setEntryProvider(entryProvider);
        calendar.setLocale(Locale.of("sl"));
        // Dodaj slog za boljšo vidnost na ozadju
        calendar.getStyle().set("background-color", "rgba(255, 255, 255, 0.95)");
        calendar.getStyle().set("border-radius", "12px");
        calendar.getStyle().set("box-shadow", "0 4px 20px rgba(0, 0, 0, 0.2)");
        calendar.getStyle().set("padding", "1rem");
        calendar.getStyle().set("margin-top", "1rem");
        calendar.addEntryClickedListener(evt -> {
            Entry entry = evt.getEntry();
            getUI().ifPresent(ui -> ui.getPage().executeJs("alert('Zadeva: ' + $0);", entry.getTitle()));
        });
    }

    private void loadCasesForSelectedMonth() {
        Integer selectedYear = yearSelect.getValue();
        Integer selectedMonth = SLO_MONTHS.get(monthSelect.getValue().toLowerCase());

        if (selectedYear == null || selectedMonth == null) return;

        LocalDate start = LocalDate.of(selectedYear, selectedMonth, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<Entry> entries = caseRepository.findAll().stream()
                .filter(c -> c.getCreatedDate() != null)
                .filter(c -> {
                    LocalDate created = c.getCreatedDate().toLocalDate();
                    return !created.isBefore(start) && !created.isAfter(end);
                })
                .map(c -> {
                    Entry e = new Entry();

                    e.setTitle(c.getTitle());
                    e.setStart(c.getCreatedDate().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    return e;
                })
                .toList();

        entryProvider.addEntries(entries);
        calendar.gotoDate(start);
    }
}
