package com.selfdiscipline.service;

import com.selfdiscipline.model.CheckIn;
import com.selfdiscipline.model.Diary;
import com.selfdiscipline.model.Pomodoro;
import com.selfdiscipline.model.Task;
import com.selfdiscipline.model.User;
import com.selfdiscipline.model.Word;
import com.selfdiscipline.repository.CheckInRepository;
import com.selfdiscipline.repository.DiaryRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    @Mock
    private DiaryRepository diaryRepository;

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
    void importedButNeverReviewedWordsDoNotCountTowardHeat() {
        // Issue #3 Bug 5：导入词书后一题未做，当天热力必须为 0
        //（修复前这里会因为回退到 createTime 而算出整本书的单词数）。
        when(checkInRepository.findByUserIdOrderByCheckInDateDesc("u1")).thenReturn(List.of());
        when(pomodoroRepository.findByUserIdOrderByStartTimeDesc("u1")).thenReturn(List.of());
        when(taskRepository.findByOwnerUserId("u1")).thenReturn(List.of());

        Word imported = new Word();
        imported.setWord("abandon");
        imported.setCreateTime(today.atTime(10, 0)); // 今天导入
        // lastReviewTime 保持 null：从未复习
        when(wordRepository.findByUserIdOrderByCreateTimeDesc("u1")).thenReturn(List.of(imported));

        Map<String, Map<String, Integer>> data = calendarService.getCalendarData("alice", today, today);

        assertNull(data.get("2026-09-20"));
    }

    @Test
    void dayDetailsExcludeImportedButNeverReviewedWords() {
        // Issue #3 Bug 5 的另一半：热力修好后，日详情这条路径（GET /api/calendar/day
        // → 日历抽屉「学习了 N 个单词」）曾被漏掉，仍然把"导入"显示成"学过"。
        when(checkInRepository.findByUserIdOrderByCheckInDateDesc("u1")).thenReturn(List.of());
        when(pomodoroRepository.findByUserIdOrderByStartTimeDesc("u1")).thenReturn(List.of());
        when(taskRepository.findByOwnerUserId("u1")).thenReturn(List.of());
        when(diaryRepository.findByUserIdOrderByDiaryDateDesc("u1")).thenReturn(List.of());

        Word imported = new Word();
        imported.setId("w1");
        imported.setWord("abandon");
        imported.setCreateTime(today.atTime(10, 0)); // 今天导入，但从未复习
        when(wordRepository.findByUserIdOrderByCreateTimeDesc("u1")).thenReturn(List.of(imported));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> words =
                (List<Map<String, Object>>) calendarService.getDayDetails("alice", today).get("words");

        assertTrue(words.isEmpty(), "从未复习的单词不应出现在日详情的单词列表里");
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

    @Test
    void incompleteTasksDoNotCountTowardHeat() {
        when(checkInRepository.findByUserIdOrderByCheckInDateDesc("u1")).thenReturn(List.of());
        when(pomodoroRepository.findByUserIdOrderByStartTimeDesc("u1")).thenReturn(List.of());
        when(wordRepository.findByUserIdOrderByCreateTimeDesc("u1")).thenReturn(List.of());

        Task open = new Task();
        open.setDueDate(today.atTime(18, 0));
        open.setStatus("todo");
        Task done = new Task();
        done.setDueDate(today.atTime(19, 0));
        done.setStatus("done");
        when(taskRepository.findByOwnerUserId("u1")).thenReturn(List.of(open, done));

        Map<String, Integer> day = calendarService.getCalendarData("alice", today, today).get("2026-09-20");
        assertEquals(1, day.get("task"));
    }

    @Test
    void restPomodorosDoNotCountTowardHeat() {
        when(checkInRepository.findByUserIdOrderByCheckInDateDesc("u1")).thenReturn(List.of());
        when(wordRepository.findByUserIdOrderByCreateTimeDesc("u1")).thenReturn(List.of());
        when(taskRepository.findByOwnerUserId("u1")).thenReturn(List.of());

        Pomodoro work = pomodoro(today.atTime(9, 0));
        Pomodoro rest = pomodoro(today.atTime(10, 0));
        rest.setType("break");
        Pomodoro legacy = pomodoro(today.atTime(11, 0));
        legacy.setType(null);
        when(pomodoroRepository.findByUserIdOrderByStartTimeDesc("u1")).thenReturn(List.of(work, rest, legacy));

        Map<String, Integer> day = calendarService.getCalendarData("alice", today, today).get("2026-09-20");
        assertEquals(2, day.get("pomodoro"));
    }

    @Test
    void dayDetailsIncludeDiaryWithoutChangingHeat() {
        when(checkInRepository.findByUserIdOrderByCheckInDateDesc("u1")).thenReturn(List.of());
        when(pomodoroRepository.findByUserIdOrderByStartTimeDesc("u1")).thenReturn(List.of());
        when(wordRepository.findByUserIdOrderByCreateTimeDesc("u1")).thenReturn(List.of());
        when(taskRepository.findByOwnerUserId("u1")).thenReturn(List.of());
        Diary diary = new Diary();
        diary.setId("d1");
        diary.setDiaryDate("2026-09-20");
        diary.setContent("收口");
        diary.setMood("happy");
        when(diaryRepository.findByUserIdOrderByDiaryDateDesc("u1")).thenReturn(List.of(diary));

        Map<String, Integer> heat = calendarService.getCalendarData("alice", today, today).get("2026-09-20");
        assertNull(heat);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> diaries =
                (List<Map<String, Object>>) calendarService.getDayDetails("alice", today).get("diaries");
        assertEquals(1, diaries.size());
        assertEquals("收口", diaries.get(0).get("content"));
        assertEquals("happy", diaries.get(0).get("mood"));
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

        // 单词热力只认"复习时间"，createTime 不再参与统计（Issue #3 Bug 5）。
        // 这里故意让两个时间戳交叉，使 aggregatesCountsForTheSameDay 同时验证
        // "按复习日归集、忽略导入日"。
        Word todayWord = new Word();
        todayWord.setCreateTime(outOfRange.atTime(10, 0));
        todayWord.setLastReviewTime(inRange.atTime(10, 0));
        Word oldWord = new Word();
        oldWord.setCreateTime(inRange.atTime(10, 0));
        oldWord.setLastReviewTime(outOfRange.atTime(10, 0));
        when(wordRepository.findByUserIdOrderByCreateTimeDesc("u1"))
                .thenReturn(List.of(todayWord, oldWord));

        Task todayTask = new Task();
        todayTask.setDueDate(inRange.atTime(18, 0));
        todayTask.setStatus("done");
        Task oldTask = new Task();
        oldTask.setDueDate(outOfRange.atTime(18, 0));
        oldTask.setStatus("done");
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
