package com.selfdiscipline.service;

import com.selfdiscipline.exception.ApiException;
import com.selfdiscipline.model.CheckIn;
import com.selfdiscipline.model.Diary;
import com.selfdiscipline.model.Pomodoro;
import com.selfdiscipline.model.Task;
import com.selfdiscipline.model.Word;
import com.selfdiscipline.repository.CheckInRepository;
import com.selfdiscipline.repository.DiaryRepository;
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

    @Autowired
    private DiaryRepository diaryRepository;

    private static final DateTimeFormatter DAY_KEY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Map<String, Map<String, Integer>> getCalendarData(String username) {
        return getCalendarData(username, null, null);
    }

    public Map<String, Map<String, Integer>> getCalendarData(String username, LocalDate start, LocalDate end) {
        String userId = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("用户不存在")).getId();

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
                .filter(CalendarService::isFocusPomodoro)
                .filter(p -> within(p.getStartTime().toLocalDate(), startInclusive, endInclusive))
                .forEach(pomodoro -> {
                    String dateKey = pomodoro.getStartTime().toLocalDate().format(DAY_KEY);
                    result.computeIfAbsent(dateKey, k -> new HashMap<>())
                            .put("pomodoro", result.get(dateKey).getOrDefault("pomodoro", 0) + 1);
                });

        // 背单词数据：只计真实复习过的单词，按"最后复习时间"归日。
        // 注意：这里绝不能回退到 createTime。刚导入词书时所有单词都没有复习记录，
        // 一旦回退，整本书会被算成"当天已学习"（Issue #3 Bug 5：导入 108 词的词书
        // 后一题未做，首页热力就凭空 +108）。
        List<Word> words = wordRepository.findByUserIdOrderByCreateTimeDesc(userId);
        words.stream()
                .filter(w -> w.getLastReviewTime() != null)
                .map(w -> w.getLastReviewTime().toLocalDate())
                .filter(d -> within(d, startInclusive, endInclusive))
                .forEach(d -> {
                    String dateKey = d.format(DAY_KEY);
                    result.computeIfAbsent(dateKey, k -> new HashMap<>())
                            .put("word", result.get(dateKey).getOrDefault("word", 0) + 1);
                });

        // 任务数据（按 dueDate 优先，其次 startDate）；热力只计已完成
        List<Task> tasks = taskRepository.findByOwnerUserId(userId);
        tasks.stream()
                .map(t -> {
                    LocalDate d = firstNonNullDate(t.getDueDate(), t.getStartDate());
                    return new Object[]{t, d};
                })
                .filter(arr -> arr[1] != null)
                .filter(arr -> within((LocalDate) arr[1], startInclusive, endInclusive))
                .filter(arr -> "done".equalsIgnoreCase(((Task) arr[0]).getStatus()))
                .forEach(arr -> {
                    LocalDate d = (LocalDate) arr[1];
                    String dateKey = d.format(DAY_KEY);
                    result.computeIfAbsent(dateKey, k -> new HashMap<>())
                            .put("task", result.get(dateKey).getOrDefault("task", 0) + 1);
                });

        return result;
    }

    static boolean isFocusPomodoro(Pomodoro pomodoro) {
        if (pomodoro == null) {
            return false;
        }
        String type = pomodoro.getType();
        return type == null || type.isBlank() || "work".equalsIgnoreCase(type);
    }

    public Map<String, Object> getDayDetails(String username, LocalDate date) {
        String userId = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("用户不存在")).getId();
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
        // Words：与热力口径保持一致，只认复习时间（Issue #3 Bug 5）。
        // 这里曾有一份与 getCalendarData 完全相同的 createTime 回退，导致"热力已修好、
        // 但点开日历当天抽屉仍写着'学习了 N 个单词'"——导入词书并不等于学过。
        List<Word> words = wordRepository.findByUserIdOrderByCreateTimeDesc(userId)
                .stream()
                .filter(w -> w.getLastReviewTime() != null
                        && date.equals(w.getLastReviewTime().toLocalDate()))
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
        List<Diary> diaries = diaryRepository.findByUserIdOrderByDiaryDateDesc(userId).stream()
                .filter(d -> date.toString().equals(d.getDiaryDate()))
                .collect(Collectors.toList());
        res.put("diaries", diaries.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("mood", d.getMood());
            m.put("content", d.getContent() == null ? "" : d.getContent());
            m.put("updatedAt", safeIso(d.getUpdatedAt()));
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











