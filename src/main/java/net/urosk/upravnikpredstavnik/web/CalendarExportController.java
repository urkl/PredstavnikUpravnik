// FILE: src/main/java/net/urosk/upravnikpredstavnik/web/CalendarExportController.java
package net.urosk.upravnikpredstavnik.web;

import jakarta.servlet.http.HttpServletResponse;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.urosk.upravnikpredstavnik.service.CalendarExportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class CalendarExportController {

    private final CalendarExportService calendarExportService;

    public CalendarExportController(CalendarExportService calendarExportService) {
        this.calendarExportService = calendarExportService;
    }

    @GetMapping(value = "/public/calendar.ics", produces = "text/calendar")
    public void getCalendarFeed(HttpServletResponse response) throws IOException {
        // Pokličemo servis, da zgradi koledar
        Calendar calendar = calendarExportService.createCalendarFeed();

        // Pripravimo HTTP odziv
        response.setContentType("text/calendar; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"BlokApp_Koledar.ics\"");

        // Zapišemo vsebino koledarja v odziv
        CalendarOutputter outputter = new CalendarOutputter();
        outputter.output(calendar, response.getWriter());
    }
}