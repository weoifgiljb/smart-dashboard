package com.selfdiscipline.service;

import com.selfdiscipline.model.CheckIn;
import com.selfdiscipline.model.Pomodoro;
import com.selfdiscipline.model.User;
import com.selfdiscipline.model.Word;
import com.selfdiscipline.repository.CheckInRepository;
import com.selfdiscipline.repository.PomodoroRepository;
import com.selfdiscipline.repository.UserRepository;
import com.selfdiscipline.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private WordService wordService;

    @Autowired
    private PomodoroService pomodoroService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private PomodoroRepository pomodoroRepository;

    @Autowired
    private WordRepository wordRepository;

    public Map<String, Object> getStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        String userId = user.getId();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("checkInDays", checkInService.getConsecutiveDays(userId));
        stats.put("wordCount", wordService.getWordCount(userId));
        stats.put("pomodoroCount", pomodoroService.getTotalCount(userId));
        stats.put("totalDays", checkInService.getTotalDays(userId));
        return stats;
    }

    public Map<String, Object> getTodayTasks(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        String userId = user.getId();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(today, LocalTime.MAX);

        Map<String, Object> tasks = new HashMap<>();
        // 今日是否打卡
        tasks.put("hasCheckedIn", checkInRepository.existsByUserIdAndCheckInDate(userId, today));
        // 今日需学习的单词数：未完成且应在今天或更早复习（dueDate <= 今日结束）
        List<Word> dueToday = wordRepository.findByUserIdAndStatusNotAndDueDateLessThanEqualOrderByDueDateAsc(
                userId, "done", endOfDay
        );
        tasks.put("todayWordCount", dueToday.size());
        // 今日完成的番茄钟数
        long todayPomodoroCount = pomodoroRepository.countByUserIdAndStartTimeBetween(userId, startOfDay, endOfDay);
        tasks.put("todayPomodoroCount", todayPomodoroCount);

        return tasks;
    }

    public List<Map<String, Object>> getRecentActivities(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        String userId = user.getId();

        List<Map<String, Object>> activities = new ArrayList<>();
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);

        // 获取最近7天的打卡记录
        List<CheckIn> recentCheckIns = checkInRepository.findByUserIdOrderByCheckInDateDesc(userId).stream()
                .filter(checkIn -> !checkIn.getCheckInDate().isBefore(sevenDaysAgo))
                .collect(Collectors.toList());
        for (CheckIn checkIn : recentCheckIns) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "checkin");
            activity.put("title", "完成打卡");
            activity.put("date", checkIn.getCheckInDate().toString());
            activity.put("time", checkIn.getCreateTime().toString());
            activities.add(activity);
        }

        // 获取最近7天的番茄钟记录
        List<Pomodoro> recentPomodoros = pomodoroRepository.findByUserIdOrderByStartTimeDesc(userId).stream()
                .filter(pomodoro -> !pomodoro.getStartTime().toLocalDate().isBefore(sevenDaysAgo))
                .limit(10)
                .collect(Collectors.toList());
        for (Pomodoro pomodoro : recentPomodoros) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "pomodoro");
            activity.put("title", "完成" + (pomodoro.getType().equals("work") ? "工作" : "休息") + "番茄钟 (" + pomodoro.getDuration() + "分钟)");
            activity.put("date", pomodoro.getStartTime().toLocalDate().toString());
            activity.put("time", pomodoro.getStartTime().toString());
            activities.add(activity);
        }

        // 获取最近7天的单词复习记录
        List<Word> recentWords = wordRepository.findByUserIdOrderByCreateTimeDesc(userId).stream()
                .filter(word -> {
                    LocalDate wordDate = word.getLastReviewTime() != null
                            ? word.getLastReviewTime().toLocalDate()
                            : word.getCreateTime().toLocalDate();
                    return !wordDate.isBefore(sevenDaysAgo);
                })
                .limit(10)
                .collect(Collectors.toList());
        for (Word word : recentWords) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "word");
            activity.put("title", "学习单词: " + word.getWord());
            LocalDate wordDate = word.getLastReviewTime() != null
                    ? word.getLastReviewTime().toLocalDate()
                    : word.getCreateTime().toLocalDate();
            activity.put("date", wordDate.toString());
            activity.put("time", (word.getLastReviewTime() != null ? word.getLastReviewTime() : word.getCreateTime()).toString());
            activities.add(activity);
        }

        // 按时间倒序排序
        activities.sort((a, b) -> {
            String timeA = (String) a.get("time");
            String timeB = (String) b.get("time");
            return timeB.compareTo(timeA);
        });

        // 只返回最近10条
        return activities.stream().limit(10).collect(Collectors.toList());
    }
}

