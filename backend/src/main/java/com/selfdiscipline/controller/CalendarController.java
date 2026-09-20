package com.selfdiscipline.controller;

import com.selfdiscipline.service.CalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Map<String, Integer>>> getCalendarData(
            Authentication authentication,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end
    ) {
        LocalDate startDate = null;
        LocalDate endDate = null;
        if (start != null && !start.isBlank()) {
            startDate = LocalDate.parse(start);
        }
        if (end != null && !end.isBlank()) {
            endDate = LocalDate.parse(end);
        }
        return ResponseEntity.ok(calendarService.getCalendarData(authentication.getName(), startDate, endDate));
    }

    @GetMapping("/day")
    public ResponseEntity<Map<String, Object>> getDayDetails(
            Authentication authentication,
            @RequestParam String date
    ) {
        LocalDate d = LocalDate.parse(date);
        return ResponseEntity.ok(calendarService.getDayDetails(authentication.getName(), d));
    }
}
