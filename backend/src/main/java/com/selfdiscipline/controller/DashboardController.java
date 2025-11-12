package com.selfdiscipline.controller;

import com.selfdiscipline.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(Authentication authentication) {
        Map<String, Object> stats = dashboardService.getStats(authentication.getName());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/today-tasks")
    public ResponseEntity<Map<String, Object>> getTodayTasks(Authentication authentication) {
        Map<String, Object> tasks = dashboardService.getTodayTasks(authentication.getName());
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/recent-activities")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivities(Authentication authentication) {
        List<Map<String, Object>> activities = dashboardService.getRecentActivities(authentication.getName());
        return ResponseEntity.ok(activities);
    }
}



