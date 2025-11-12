package com.selfdiscipline.service;

import com.selfdiscipline.dto.PomodoroRequest;
import com.selfdiscipline.model.Pomodoro;
import com.selfdiscipline.model.User;
import com.selfdiscipline.repository.PomodoroRepository;
import com.selfdiscipline.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PomodoroService {

    @Autowired
    private PomodoroRepository pomodoroRepository;

    @Autowired
    private UserRepository userRepository;

    public Pomodoro startPomodoro(String username, PomodoroRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Pomodoro pomodoro = new Pomodoro();
        pomodoro.setUserId(user.getId());
        pomodoro.setDuration(request.getDuration());
        pomodoro.setType(request.getType());
        pomodoro.setEndTime(pomodoro.getStartTime().plusMinutes(request.getDuration()));

        return pomodoroRepository.save(pomodoro);
    }

    public Map<String, Object> getStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        long todayCount = pomodoroRepository.countByUserIdAndStartTimeBetween(user.getId(), startOfDay, endOfDay);
        long totalCount = pomodoroRepository.countByUserId(user.getId());

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayCount", todayCount);
        stats.put("totalCount", totalCount);
        return stats;
    }

    public List<Pomodoro> getHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return pomodoroRepository.findByUserIdOrderByStartTimeDesc(user.getId());
    }

    public int getTotalCount(String userId) {
        return (int) pomodoroRepository.countByUserId(userId);
    }
}
