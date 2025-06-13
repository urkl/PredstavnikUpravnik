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
import net.urosk.upravnikpredstavnik.config.AppProcessProperties;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import org.vaadin.stefan.fullcalendar.Entry;
import org.vaadin.stefan.fullcalendar.FullCalendar;
import org.vaadin.stefan.fullcalendar.FullCalendarBuilder;
import org.vaadin.stefan.fullcalendar.dataprovider.InMemoryEntryProvider;
import com.vaadin.flow.component.button.Button; // NOV UVOZ
import com.vaadin.flow.component.button.ButtonVariant; // NOV UVOZ
import com.vaadin.flow.component.icon.Icon; // NOV UVOZ
import com.vaadin.flow.component.icon.VaadinIcon; // NOV UVOZ


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Route(value = "koledar", layout = MainLayout.class)
@PageTitle("Koledar")
@PermitAll
public class CalendarView extends VerticalLayout {

    private final CaseRepository caseRepository;
    private final AppProcessProperties appProcessProperties;
    private final InMemoryEntryProvider<Entry> entryProvider = new InMemoryEntryProvider<>();
    private final FullCalendar calendar = FullCalendarBuilder.create().build();
    ;
    private final ComboBox<Integer> yearSelect = new ComboBox<>("Leto");
    private final ComboBox<String> monthSelect = new ComboBox<>("Mesec");

    // NOV DODATEK: Gumba za navigacijo po mesecih
    private final Button prevMonthButton = new Button(new Icon(VaadinIcon.ANGLE_LEFT));
    private final Button nextMonthButton = new Button(new Icon(VaadinIcon.ANGLE_RIGHT));


    private static final Map<String, Integer> SLO_MONTHS = new HashMap<>();
    static {
        for (int i = 1; i <= 12; i++) {
            SLO_MONTHS.put(java.time.Month.of(i).getDisplayName(TextStyle.FULL, new Locale("sl", "SI")).toLowerCase(), i);
        }
    }


    public CalendarView(CaseRepository caseRepository, AppProcessProperties appProcessProperties) {
        this.caseRepository = caseRepository;
        this.appProcessProperties = appProcessProperties;

        setSizeFull();
        setPadding(true);
        addClassNames("calendar-view", LumoUtility.Padding.MEDIUM);

        configureTopBar();
        configureCalendar();

        add(createTopBar(), calendar);
        loadCasesForSelectedMonth();
    }

    private void configureTopBar() {
        int currentYear = LocalDate.now().getYear();
        List<Integer> years = IntStream.rangeClosed(currentYear - 5, currentYear + 3)
                .boxed().collect(Collectors.toList());
        yearSelect.setItems(years);
        yearSelect.setValue(currentYear);
        yearSelect.setWidth("120px");

        List<String> months = new ArrayList<>(SLO_MONTHS.keySet());
        months.sort(Comparator.comparingInt(s -> SLO_MONTHS.get(s.toLowerCase())));
        monthSelect.setItems(months);
        monthSelect.setValue(LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, new Locale("sl", "SI")));
        monthSelect.setWidth("140px");

        yearSelect.addValueChangeListener(e -> loadCasesForSelectedMonth());
        monthSelect.addValueChangeListener(e -> loadCasesForSelectedMonth());

        // NOV DODATEK: Konfiguracija gumbov za navigacijo
        prevMonthButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        nextMonthButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        prevMonthButton.addClickListener(e -> navigateMonth(-1)); // Prejšnji mesec
        nextMonthButton.addClickListener(e -> navigateMonth(1)); // Naslednji mesec
    }

    private HorizontalLayout createTopBar() {
        HorizontalLayout topBar = new HorizontalLayout(prevMonthButton, monthSelect, yearSelect, nextMonthButton); // DODANO: Gumba
        topBar.addClassName("layout");
        topBar.setAlignItems(FlexComponent.Alignment.BASELINE);
        topBar.setSpacing(true);
        topBar.setPadding(false);
        topBar.setWidthFull(); // Naj se razširi, da gumba prideta na robove
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER); // Centrirajte vsebino, gumba pa se bosta pomaknila na robove zaradi setWidthFull
        return topBar;
    }

    private void configureCalendar() {
        calendar.setSizeFull();
        calendar.setEntryProvider(entryProvider);
        calendar.setLocale(Locale.of("sl"));
        calendar.getStyle().set("background-color", "rgba(255, 255, 255, 0.95)");
        calendar.getStyle().set("border-radius", "12px");
        calendar.getStyle().set("box-shadow", "0 4px 20px rgba(0, 0, 0, 0.2)");
        calendar.getStyle().set("padding", "1rem");
        calendar.getStyle().set("margin-top", "1rem");
        calendar.addEntryClickedListener(evt -> {
            Entry entry = evt.getEntry();
            String statusKey = (String) entry.getCustomProperty("status");
            String buildingsValue = (String) entry.getCustomProperty("buildings");

            String statusDisplayName = (statusKey != null && appProcessProperties.getStatuses().containsKey(statusKey))
                    ? appProcessProperties.getStatuses().get(statusKey)
                    : "Neznan status";

            String details = "Zadeva: " + entry.getTitle() + "\n";
            details += "Status: " + statusDisplayName + "\n";
            details += "Objekt(i): " + (buildingsValue != null ? buildingsValue : "Ni določenih objektov") + "\n";
            details += "Od: " + (entry.getStart() != null ? entry.getStart().format(java.time.format.DateTimeFormatter.ofPattern("d.M.yyyy", new Locale("sl", "SI"))) : "N/A") + "\n";
            details += "Do: " + (entry.getEnd() != null ? entry.getEnd().format(java.time.format.DateTimeFormatter.ofPattern("d.M.yyyy", new Locale("sl", "SI"))) : "N/A");

            String finalDetails = details;
            getUI().ifPresent(ui -> ui.getPage().executeJs("alert($0);", finalDetails));
        });
    }

    private void loadCasesForSelectedMonth() {
        Integer selectedYear = yearSelect.getValue();
        Integer selectedMonth = SLO_MONTHS.get(monthSelect.getValue().toLowerCase());

        if (selectedYear == null || selectedMonth == null) {
            return;
        }

        LocalDate startOfMonth = LocalDate.of(selectedYear, selectedMonth, 1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

        List<Entry> entries = caseRepository.findAll().stream()
                .filter(c -> {
                    boolean startsInMonth = c.getStartDate() != null && !c.getStartDate().toLocalDate().isAfter(endOfMonth) && !c.getStartDate().toLocalDate().isBefore(startOfMonth);
                    boolean endsInMonth = c.getEndDate() != null && !c.getEndDate().toLocalDate().isBefore(startOfMonth) && !c.getEndDate().toLocalDate().isAfter(endOfMonth);
                    boolean createdInMonth = c.getCreatedDate() != null && !c.getCreatedDate().toLocalDate().isAfter(endOfMonth) && !c.getCreatedDate().toLocalDate().isBefore(startOfMonth);

                    return startsInMonth || endsInMonth || createdInMonth;
                })
                .map(c -> {
                    Entry e = new Entry();

                    String buildingNames = c.getBuildings() != null && !c.getBuildings().isEmpty() ?
                            c.getBuildings().stream().map(b -> b.getName()).collect(Collectors.joining(", ")) : "Ni določenih objektov";
                    String titlePrefix = c.getStatus() != null ? appProcessProperties.getStatuses().get(c.getStatus()) : "Zadeva";

                    e.setTitle("🗓️ " + titlePrefix + ": " + c.getTitle() + " (" + buildingNames + ")");
                    e.setAllDay(true);

                    if (c.getStartDate() != null) {
                        e.setStart(c.getStartDate().atZone(ZoneId.systemDefault()).toInstant());
                    } else {
                        e.setStart(c.getCreatedDate() != null ? c.getCreatedDate().atZone(ZoneId.systemDefault()).toInstant() : LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
                    }

                    if (c.getEndDate() != null) {
                        e.setEnd(c.getEndDate().atZone(ZoneId.systemDefault()).toInstant());
                    } else {
                        e.setEnd(e.getStart());
                    }

                    String color = switch (c.getStatus()) {
                        case "PREDLOG", "V_PREGLEDU" -> "#9C27B0";
                        case "POTRJENO", "V_DELU" -> "#FF9800";
                        case "ZAKLJUCENO" -> "#4CAF50";
                        default -> "#2196F3";
                    };
                    e.setColor(color);
                    e.setTextColor("white");

                    e.setCustomProperty("status", c.getStatus());
                    e.setCustomProperty("buildings", buildingNames);

                    return e;
                })
                .collect(Collectors.toList());

        entryProvider.removeAllEntries();
        entryProvider.addEntries(entries);
        calendar.gotoDate(startOfMonth);
    }

    // NOV DODATEK: Metoda za navigacijo po mesecih
    private void navigateMonth(int monthOffset) {
        YearMonth currentSelectedMonth = YearMonth.of(yearSelect.getValue(), SLO_MONTHS.get(monthSelect.getValue().toLowerCase()));
        YearMonth newMonth = currentSelectedMonth.plusMonths(monthOffset);

        // Preveri, ali se je spremenilo leto in posodobi yearSelect
        if (newMonth.getYear() != yearSelect.getValue()) {
            yearSelect.setValue(newMonth.getYear());
        }

        // Posodobi monthSelect
        monthSelect.setValue(newMonth.getMonth().getDisplayName(TextStyle.FULL, new Locale("sl", "SI")));

        // Ponovno naloži dogodke za novi mesec
        loadCasesForSelectedMonth();
    }
}