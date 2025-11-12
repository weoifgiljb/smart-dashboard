package com.selfdiscipline.controller;

import com.selfdiscipline.dto.PomodoroRequest;
import com.selfdiscipline.model.Pomodoro;
import com.selfdiscipline.service.PomodoroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/pomodoro")
public class PomodoroController {

    @Autowired
    private PomodoroService pomodoroService;

    @PostMapping
    public ResponseEntity<Pomodoro> startPomodoro(@RequestBody PomodoroRequest request, Authentication authentication) {
        Pomodoro pomodoro = pomodoroService.startPomodoro(authentication.getName(), request);
        return ResponseEntity.ok(pomodoro);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(Authentication authentication) {
        Map<String, Object> stats = pomodoroService.getStats(authentication.getName());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(Authentication authentication) {
        return ResponseEntity.ok(pomodoroService.getHistory(authentication.getName()));
    }
}











