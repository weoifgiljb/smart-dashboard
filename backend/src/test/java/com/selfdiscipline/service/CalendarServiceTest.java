package com.selfdiscipline.service;

import com.selfdiscipline.model.CheckIn;
import com.selfdiscipline.model.Pomodoro;
import com.selfdiscipline.model.Task;
import com.selfdiscipline.model.User;
import com.selfdiscipline.model.Word;
import com.selfdiscipline.repository.CheckInRepository;
import com.selfdiscipline.repository.PomodoroRepository;
import com.selfdiscipline.repository.TaskRepository;
import com.selfdiscipline.repository.UserRepository;
import com.selfdiscipline.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private CheckInRepository checkInRepository;
    @Mock
    private PomodoroRepository pomodoroRepository;
    @Mock
    private WordRepository wordRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private CalendarService calendarService;

    private final LocalDate today = LocalDate.of(2026, 9, 20);
    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId("u1");
        alice.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
    }

    @Test
    void aggregatesCountsForTheSameDay() {
        stubSources(today, today.minusDays(10));

        Map<String, Map<String, Integer>> data = calendarService.getCalendarData("alice", today, today);
        Map<String, Integer> day = data.get("2026-09-20");

        assertEquals(1, day.get("checkin"));
        assertEquals(2, day.get("pomodoro"));
        assertEquals(1, day.get("word"));
        assertEquals(1, day.get("task"));
        assertFalse(data.containsKey("2026-09-10"));
    }

    @Test
    void swapsInvertedRange() {
        stubSources(today, today.minusDays(10));

        Map<String, Map<String, Integer>> data = calendarService.getCalendarData(
                "alice", today.plusDays(1), today.minusDays(1)
        );

        assertEquals(1, data.get("2026-09-20").get("checkin"));
        assertNull(data.get("2026-09-10"));
    }

    @Test
    void excludesRecordsOutsideRange() {
        stubSources(today, today.minusDays(10));

        Map<String, Map<String, Integer>> data = calendarService.getCalendarData(
                "alice", today.minusDays(2), today
        );

        assertEquals(1, data.get("2026-09-20").get("checkin"));
        assertFalse(data.containsKey("2026-09-10"));
    }

    private void stubSources(LocalDate inRange, LocalDate outOfRange) {
        CheckIn todayCheck = new CheckIn();
        todayCheck.setCheckInDate(inRange);
        CheckIn oldCheck = new CheckIn();
        oldCheck.setCheckInDate(outOfRange);
        when(checkInRepository.findByUserIdOrderByCheckInDateDesc("u1"))
                .thenReturn(List.of(todayCheck, oldCheck));

        Pomodoro p1 = pomodoro(inRange.atTime(9, 0));
        Pomodoro p2 = pomodoro(inRange.atTime(14, 0));
        Pomodoro oldP = pomodoro(outOfRange.atTime(9, 0));
        when(pomodoroRepository.findByUserIdOrderByStartTimeDesc("u1"))
                .thenReturn(List.of(p1, p2, oldP));

        Word todayWord = new Word();
        todayWord.setCreateTime(inRange.atTime(10, 0));
        Word oldWord = new Word();
        oldWord.setCreateTime(outOfRange.atTime(10, 0));
        when(wordRepository.findByUserIdOrderByCreateTimeDesc("u1"))
                .thenReturn(List.of(todayWord, oldWord));

        Task todayTask = new Task();
        todayTask.setDueDate(inRange.atTime(18, 0));
        Task oldTask = new Task();
        oldTask.setDueDate(outOfRange.atTime(18, 0));
        when(taskRepository.findByOwnerUserId("u1")).thenReturn(List.of(todayTask, oldTask));
    }

    private static Pomodoro pomodoro(LocalDateTime start) {
        Pomodoro pomodoro = new Pomodoro();
        pomodoro.setStartTime(start);
        pomodoro.setDuration(25);
        pomodoro.setType("work");
        return pomodoro;
    }
}
