// FILE: src/main/java/net/urosk/upravnikpredstavnik/service/CalendarExportService.java
package net.urosk.upravnikpredstavnik.service;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.urosk.upravnikpredstavnik.data.entity.Building;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CalendarExportService {

    private final CaseRepository caseRepository;

    public CalendarExportService(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    /**
     * Ustvari iCalendar feed z vsemi zadevami, ki imajo določen časovni okvir.
     * @return iCalendar objekt.
     */
    public Calendar createCalendarFeed() {
        // --- POPRAVEK: Pravilna inicializacija koledarja za ical4j v4+ ---
        Calendar calendar = new Calendar()
                .withProdId("-//BlokApp - urosk.net//iCal4j 4.1.1//SL")
                .withDefaults() // Ta metoda doda obvezne lastnosti, kot sta Version in CalScale
                .getFluentTarget();

        caseRepository.findAll().stream()
                .filter(c -> c.getStartDate() != null)
                .map(this::createVEventFromCase)
                .filter(Objects::nonNull)
                .forEach(calendar::add); // Uporabimo direktno metodo add

        return calendar;
    }

    /**
     * Pretvori entiteto Case v iCalendar VEvent.
     * Uporablja moderno java.time API, ki ga podpira ical4j v4.x.
     * @param caseItem Zadeva iz baze podatkov.
     * @return VEvent objekt.
     */
    private VEvent createVEventFromCase(Case caseItem) {
        String eventName = caseItem.getTitle();
        LocalDateTime start = caseItem.getStartDate();
        LocalDateTime end = (caseItem.getEndDate() != null) ? caseItem.getEndDate() : start.plusHours(1);

        // Uporabimo nov, tekoč (fluent) način gradnje dogodka
        VEvent event = new VEvent(start, end, eventName);

        // Dodajanje lastnosti z metodo .add()
        event.add(new Uid(caseItem.getId()));

        String descriptionText = caseItem.getDescription();
        if (caseItem.getBuildings() != null && !caseItem.getBuildings().isEmpty()) {
            descriptionText += "\n\nObjekti: " + caseItem.getBuildings().stream()
                    .map(Building::getName)
                    .collect(Collectors.joining(", "));
        }
        event.add(new Description(descriptionText));

        return event;
    }
}