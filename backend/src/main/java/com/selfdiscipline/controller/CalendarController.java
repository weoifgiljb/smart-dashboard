package com.selfdiscipline.controller;

import com.selfdiscipline.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/calendar")
public class CalendarController {

    @Autowired
    private CalendarService calendarService;

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
        Map<String, Map<String, Integer>> data = calendarService.getCalendarData(authentication.getName(), startDate, endDate);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/day")
    public ResponseEntity<Map<String, Object>> getDayDetails(
            Authentication authentication,
            @RequestParam String date
    ) {
        try {
            LocalDate d = LocalDate.parse(date);
            return ResponseEntity.ok(calendarService.getDayDetails(authentication.getName(), d));
        } catch (java.time.format.DateTimeParseException ex) {
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("error", "Invalid date");
            body.put("hint", "Use format yyyy-MM-dd");
            return ResponseEntity.badRequest().body(body);
        } catch (Exception ex) {
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("error", ex.getClass().getSimpleName());
            body.put("message", ex.getMessage());
            return ResponseEntity.status(500).body(body);
        }
    }
}











