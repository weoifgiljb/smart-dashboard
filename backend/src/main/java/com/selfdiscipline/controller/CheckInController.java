package com.selfdiscipline.controller;

import com.selfdiscipline.service.CheckInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/checkin")
public class CheckInController {

    @Autowired
    private CheckInService checkInService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> checkIn(Authentication authentication) {
        Map<String, Object> result = checkInService.checkIn(authentication.getName());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(Authentication authentication) {
        Map<String, Object> stats = checkInService.getStats(authentication.getName());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(checkInService.getHistory(authentication.getName(), page, size));
    }

    @GetMapping("/history-with-heatvalue")
    public ResponseEntity<?> getHistoryWithHeatValue(Authentication authentication) {
        return ResponseEntity.ok(checkInService.getCheckInHistoryWithHeatValue(authentication.getName()));
    }
}











