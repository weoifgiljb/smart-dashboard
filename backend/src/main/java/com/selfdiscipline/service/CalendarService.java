package com.selfdiscipline.service;

import com.selfdiscipline.model.CheckIn;
import com.selfdiscipline.model.Pomodoro;
import com.selfdiscipline.model.Task;
import com.selfdiscipline.model.Word;
import com.selfdiscipline.repository.CheckInRepository;
import com.selfdiscipline.repository.PomodoroRepository;
import com.selfdiscipline.repository.TaskRepository;
import com.selfdiscipline.repository.UserRepository;
import com.selfdiscipline.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private PomodoroRepository pomodoroRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    private static final DateTimeFormatter DAY_KEY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Map<String, Map<String, Integer>> getCalendarData(String username) {
        return getCalendarData(username, null, null);
    }

    public Map<String, Map<String, Integer>> getCalendarData(String username, LocalDate start, LocalDate end) {
        String userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在")).getId();

        Map<String, Map<String, Integer>> result = new HashMap<>();
        LocalDate s = start;
        LocalDate e = end;
        if (s != null && e != null && e.isBefore(s)) {
            LocalDate tmp = s;
            s = e;
            e = tmp;
        }
        final LocalDate startInclusive = s;
        final LocalDate endInclusive = e;

        // 打卡数据
        List<CheckIn> checkIns = checkInRepository.findByUserIdOrderByCheckInDateDesc(userId);
        checkIns.stream()
                .filter(c -> within(c.getCheckInDate(), startInclusive, endInclusive))
                .forEach(checkIn -> {
                    String dateKey = checkIn.getCheckInDate().format(DAY_KEY);
                    result.computeIfAbsent(dateKey, k -> new HashMap<>()).put("checkin", 1);
                });

        // 番茄钟数据
        List<Pomodoro> pomodoros = pomodoroRepository.findByUserIdOrderByStartTimeDesc(userId);
        pomodoros.stream()
                .filter(p -> p.getStartTime() != null)
                .filter(p -> within(p.getStartTime().toLocalDate(), startInclusive, endInclusive))
                .forEach(pomodoro -> {
                    String dateKey = pomodoro.getStartTime().toLocalDate().format(DAY_KEY);
                    result.computeIfAbsent(dateKey, k -> new HashMap<>())
                            .put("pomodoro", result.get(dateKey).getOrDefault("pomodoro", 0) + 1);
                });

        // 背单词数据（按最后复习时间或创建时间）
        List<Word> words = wordRepository.findByUserIdOrderByCreateTimeDesc(userId);
        words.stream()
                .map(w -> {
                    LocalDate d = w.getLastReviewTime() != null
                            ? w.getLastReviewTime().toLocalDate()
                            : (w.getCreateTime() != null ? w.getCreateTime().toLocalDate() : null);
                    return new Object[]{w, d};
                })
                .filter(arr -> arr[1] != null)
                .filter(arr -> within((LocalDate) arr[1], startInclusive, endInclusive))
                .forEach(arr -> {
                    LocalDate d = (LocalDate) arr[1];
                    String dateKey = d.format(DAY_KEY);
                    result.computeIfAbsent(dateKey, k -> new HashMap<>())
                            .put("word", result.get(dateKey).getOrDefault("word", 0) + 1);
                });

        // 任务数据（按 dueDate 优先，其次 startDate）
        List<Task> tasks = taskRepository.findByOwnerUserId(userId);
        tasks.stream()
                .map(t -> {
                    LocalDate d = firstNonNullDate(t.getDueDate(), t.getStartDate());
                    return new Object[]{t, d};
                })
                .filter(arr -> arr[1] != null)
                .filter(arr -> within((LocalDate) arr[1], startInclusive, endInclusive))
                .forEach(arr -> {
                    LocalDate d = (LocalDate) arr[1];
                    String dateKey = d.format(DAY_KEY);
                    result.computeIfAbsent(dateKey, k -> new HashMap<>())
                            .put("task", result.get(dateKey).getOrDefault("task", 0) + 1);
                });

        return result;
    }

    public Map<String, Object> getDayDetails(String username, LocalDate date) {
        String userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在")).getId();
        Map<String, Object> res = new HashMap<>();
        // Check-ins
        List<CheckIn> checkIns = checkInRepository.findByUserIdOrderByCheckInDateDesc(userId)
                .stream()
                .filter(c -> date.equals(c.getCheckInDate()))
                .collect(Collectors.toList());
        res.put("checkins", checkIns.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("date", c.getCheckInDate() == null ? null : c.getCheckInDate().format(DAY_KEY));
            return m;
        }).collect(Collectors.toList()));
        // Pomodoros
        List<Pomodoro> pomodoros = pomodoroRepository.findByUserIdOrderByStartTimeDesc(userId)
                .stream()
                .filter(p -> p.getStartTime() != null && date.equals(p.getStartTime().toLocalDate()))
                .collect(Collectors.toList());
        res.put("pomodoros", pomodoros.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("duration", p.getDuration());
            m.put("type", p.getType());
            m.put("startTime", safeIso(p.getStartTime()));
            m.put("endTime", safeIso(p.getEndTime()));
            return m;
        }).collect(Collectors.toList()));
        // Words
        List<Word> words = wordRepository.findByUserIdOrderByCreateTimeDesc(userId)
                .stream()
                .filter(w -> {
                    LocalDate d = w.getLastReviewTime() != null
                            ? w.getLastReviewTime().toLocalDate()
                            : (w.getCreateTime() != null ? w.getCreateTime().toLocalDate() : null);
                    return d != null && date.equals(d);
                })
                .collect(Collectors.toList());
        res.put("words", words.stream().map(w -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", w.getId());
            m.put("word", w.getWord());
            m.put("status", w.getStatus());
            m.put("createTime", safeIso(w.getCreateTime()));
            m.put("lastReviewTime", safeIso(w.getLastReviewTime()));
            return m;
        }).collect(Collectors.toList()));
        // Tasks
        List<Task> tasks = taskRepository.findByOwnerUserId(userId)
                .stream()
                .filter(t -> {
                    LocalDate d = firstNonNullDate(t.getDueDate(), t.getStartDate());
                    return d != null && date.equals(d);
                })
                .collect(Collectors.toList());
        res.put("tasks", tasks.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle() == null ? "" : t.getTitle());
            m.put("status", t.getStatus() == null ? "todo" : t.getStatus());
            m.put("priority", t.getPriority() == null ? "med" : t.getPriority());
            m.put("dueDate", safeIso(t.getDueDate()));
            m.put("startDate", safeIso(t.getStartDate()));
            return m;
        }).collect(Collectors.toList()));
        return res;
    }

    private boolean within(LocalDate day, LocalDate startInclusive, LocalDate endInclusive) {
        if (day == null) return false;
        if (startInclusive != null && day.isBefore(startInclusive)) return false;
        if (endInclusive != null && day.isAfter(endInclusive)) return false;
        return true;
    }

    private LocalDate firstNonNullDate(LocalDateTime due, LocalDateTime start) {
        if (due != null) return due.toLocalDate();
        if (start != null) return start.toLocalDate();
        return null;
    }

    private String safeIso(LocalDateTime dt) {
        return dt == null ? null : dt.toString();
    }
}











