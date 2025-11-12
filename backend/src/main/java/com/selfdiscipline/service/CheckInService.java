package com.selfdiscipline.service;

import com.selfdiscipline.model.CheckIn;
import com.selfdiscipline.model.User;
import com.selfdiscipline.repository.CheckInRepository;
import com.selfdiscipline.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckInService {

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CalendarService calendarService;

    public Map<String, Object> checkIn(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        LocalDate today = LocalDate.now();
        if (checkInRepository.existsByUserIdAndCheckInDate(user.getId(), today)) {
            throw new RuntimeException("今日已打卡");
        }

        CheckIn checkIn = new CheckIn();
        checkIn.setUserId(user.getId());
        checkIn.setCheckInDate(today);
        
        // 计算当日热力值：番茄 + 单词 + 任务 + 基础打卡分
        int heatValue = calculateDailyHeatValue(username, today);
        checkIn.setHeatValue(heatValue);
        
        checkInRepository.save(checkIn);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "打卡成功");
        result.put("checkInDate", today);
        result.put("heatValue", heatValue);
        return result;
    }
    
    /**
     * 计算当日热力值
     * 规则：打卡基础分 1 分 + 每个番茄 2 分 + 每个单词 1 分 + 每个任务 3 分
     */
    public int calculateDailyHeatValue(String username, LocalDate date) {
        Map<String, Map<String, Integer>> calendarData = calendarService.getCalendarData(
                username, 
                date, 
                date
        );
        
        String dateKey = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Map<String, Integer> dayData = calendarData.getOrDefault(dateKey, new HashMap<>());
        
        // 热力值计算：基础分 1 + 番茄 * 2 + 单词 * 1 + 任务 * 3
        int baseScore = 1;
        int pomodoroScore = dayData.getOrDefault("pomodoro", 0) * 2;
        int wordScore = dayData.getOrDefault("word", 0) * 1;
        int taskScore = dayData.getOrDefault("task", 0) * 3;
        
        return baseScore + pomodoroScore + wordScore + taskScore;
    }

    public Map<String, Object> getStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        LocalDate today = LocalDate.now();
        boolean hasCheckedInToday = checkInRepository.existsByUserIdAndCheckInDate(user.getId(), today);

        List<CheckIn> checkIns = checkInRepository.findByUserIdOrderByCheckInDateDesc(user.getId());
        int totalDays = checkIns.size();

        // 优化连续打卡天数计算逻辑
        int consecutiveDays = 0;
        LocalDate expectedDate = today;
        for (CheckIn checkIn : checkIns) {
            if (checkIn.getCheckInDate().equals(expectedDate)) {
                consecutiveDays++;
                expectedDate = expectedDate.minusDays(1);
            } else if (checkIn.getCheckInDate().isBefore(expectedDate)) {
                // 如果打卡日期早于期望日期，说明中间有断档，停止计算
                break;
            }
            // 如果打卡日期晚于期望日期，跳过（可能是数据异常）
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("hasCheckedInToday", hasCheckedInToday);
        stats.put("consecutiveDays", consecutiveDays);
        stats.put("totalDays", totalDays);
        
        // 如果今天已打卡，添加今日热力值
        if (hasCheckedInToday) {
            int todayHeatValue = calculateDailyHeatValue(username, today);
            stats.put("todayHeatValue", todayHeatValue);
        }
        
        return stats;
    }

    public int getConsecutiveDays(String userId) {
        List<CheckIn> checkIns = checkInRepository.findByUserIdOrderByCheckInDateDesc(userId);
        int consecutiveDays = 0;
        LocalDate expectedDate = LocalDate.now();
        for (CheckIn checkIn : checkIns) {
            if (checkIn.getCheckInDate().equals(expectedDate)) {
                consecutiveDays++;
                expectedDate = expectedDate.minusDays(1);
            } else if (checkIn.getCheckInDate().isBefore(expectedDate)) {
                // 如果打卡日期早于期望日期，说明中间有断档，停止计算
                break;
            }
            // 如果打卡日期晚于期望日期，跳过（可能是数据异常）
        }
        return consecutiveDays;
    }

    public int getTotalDays(String userId) {
        return (int) checkInRepository.findByUserIdOrderByCheckInDateDesc(userId).size();
    }

    public List<CheckIn> getHistory(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        List<CheckIn> checkIns = checkInRepository.findByUserIdOrderByCheckInDateDesc(user.getId());
        int start = page * size;
        int end = Math.min(start + size, checkIns.size());
        return checkIns.subList(Math.min(start, checkIns.size()), end);
    }
    
    /**
     * 获取打卡历史统计信息
     * 返回每天的打卡记录及对应的热力值
     */
    public List<Map<String, Object>> getCheckInHistoryWithHeatValue(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        List<CheckIn> checkIns = checkInRepository.findByUserIdOrderByCheckInDateDesc(user.getId());
        
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (CheckIn checkIn : checkIns) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", checkIn.getId());
            // 将 LocalDate 格式化为字符串，避免序列化问题
            item.put("checkInDate", checkIn.getCheckInDate().format(dateFormatter));
            // 始终以新算法为准计算；若与已存值不一致则回写，确保后续为最新值
            int computed = calculateDailyHeatValue(user.getUsername(), checkIn.getCheckInDate());
            int heatValue = computed;
            if (checkIn.getHeatValue() != computed) {
                checkIn.setHeatValue(computed);
                checkInRepository.save(checkIn);
            }
            item.put("heatValue", heatValue);
            result.add(item);
        }
        return result;
    }
}
