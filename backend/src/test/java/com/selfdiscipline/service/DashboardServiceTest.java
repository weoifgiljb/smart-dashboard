package com.selfdiscipline.service;

import com.selfdiscipline.dto.RhythmResponse;
import com.selfdiscipline.exception.ApiException;
import com.selfdiscipline.model.CheckIn;
import com.selfdiscipline.model.Diary;
import com.selfdiscipline.model.Pomodoro;
import com.selfdiscipline.model.Task;
import com.selfdiscipline.model.User;
import com.selfdiscipline.model.Word;
import com.selfdiscipline.repository.CheckInRepository;
import com.selfdiscipline.repository.DiaryRepository;
import com.selfdiscipline.repository.PomodoroRepository;
import com.selfdiscipline.repository.UserRepository;
import com.selfdiscipline.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.selfdiscipline.testsupport.MockitoArgs.nullableArg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private CheckInService checkInService;
    @Mock
    private WordService wordService;
    @Mock
    private PomodoroService pomodoroService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CheckInRepository checkInRepository;
    @Mock
    private PomodoroRepository pomodoroRepository;
    @Mock
    private WordRepository wordRepository;
    @Mock
    private CalendarService calendarService;
    @Mock
    private TaskService taskService;
    @Mock
    private DiaryRepository diaryRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId("u1");
        alice.setUsername("alice");
    }

    @Test
    void rhythmThrowsWhenUserMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> dashboardService.getRhythm("ghost"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void rhythmPrefersCheckInWhenNotCheckedIn() {
        stubUser();
        stubDueWords(List.of());
        stubOpenTasks(List.of());
        stubHeat(0, 0, 0);
        when(checkInRepository.existsByUserIdAndCheckInDate("u1", LocalDate.now())).thenReturn(false);

        RhythmResponse rhythm = dashboardService.getRhythm("alice");

        assertEquals(RhythmPlanner.CHECK_IN, rhythm.getNextAction());
        assertEquals("/calendar", rhythm.getCtaPath());
        assertEquals("立即打卡", rhythm.getCtaLabel());
        assertEquals("今天还没打卡，先记下这一天的起点。", rhythm.getReason());
    }

    @Test
    void rhythmReviewsFirstThreeDueWords() {
        stubUser();
        stubDueWords(IntStream.rangeClosed(1, 5).mapToObj(i -> word("w" + i, "word" + i)).toList());
        stubOpenTasks(List.of());
        stubHeat(0, 0, 0);
        when(checkInRepository.existsByUserIdAndCheckInDate("u1", LocalDate.now())).thenReturn(true);

        RhythmResponse rhythm = dashboardService.getRhythm("alice");

        assertEquals(RhythmPlanner.REVIEW_WORDS, rhythm.getNextAction());
        assertEquals(5, rhythm.getDueWordCount());
        assertEquals(3, rhythm.getDueWords().size());
        assertEquals("复习 3 张到期词", rhythm.getCtaLabel());
        assertTrue(rhythm.getCtaPath().contains("/vocabulary/review?ids="));
        assertTrue(rhythm.getCtaPath().contains("w1"));
        assertTrue(rhythm.getCtaPath().contains("w3"));
        assertEquals("有 5 个到期单词，先用两分钟接上记忆。", rhythm.getReason());
    }

    @Test
    void rhythmFocusesInProgressTaskWhenNoDueWords() {
        stubUser();
        stubDueWords(List.of());
        Task todo = task("t-todo", "todo", "稍后");
        Task progress = task("t-prog", "in_progress", "正在做");
        stubOpenTasks(List.of(todo, progress));
        stubHeat(0, 0, 0);
        when(checkInRepository.existsByUserIdAndCheckInDate("u1", LocalDate.now())).thenReturn(true);

        RhythmResponse rhythm = dashboardService.getRhythm("alice");

        assertEquals(RhythmPlanner.FOCUS_TASK, rhythm.getNextAction());
        assertEquals("开始专注", rhythm.getCtaLabel());
        assertEquals("/pomodoro?taskId=t-prog", rhythm.getCtaPath());
        assertNotNull(rhythm.getFocusTask());
        assertEquals("t-prog", rhythm.getFocusTask().getId());
        assertTrue(rhythm.getReason().contains("正在做"));
    }

    @Test
    void rhythmFallsBackToFirstTodoWhenNoInProgress() {
        stubUser();
        stubDueWords(List.of());
        stubOpenTasks(List.of(task("t-todo", "todo", "待办")));
        stubHeat(0, 0, 0);
        when(checkInRepository.existsByUserIdAndCheckInDate("u1", LocalDate.now())).thenReturn(true);

        RhythmResponse rhythm = dashboardService.getRhythm("alice");

        assertEquals(RhythmPlanner.FOCUS_TASK, rhythm.getNextAction());
        assertEquals("/pomodoro?taskId=t-todo", rhythm.getCtaPath());
        assertEquals("t-todo", rhythm.getFocusTask().getId());
    }

    @Test
    void rhythmFallsBackToFreeFocus() {
        stubUser();
        stubDueWords(List.of());
        stubOpenTasks(List.of());
        stubHeat(0, 0, 0);
        when(checkInRepository.existsByUserIdAndCheckInDate("u1", LocalDate.now())).thenReturn(true);
        Diary diary = new Diary();
        diary.setDiaryDate(LocalDate.now().toString());
        when(diaryRepository.findByUserIdAndDiaryDate("u1", LocalDate.now().toString()))
                .thenReturn(Optional.of(diary));

        RhythmResponse rhythm = dashboardService.getRhythm("alice");

        assertEquals(RhythmPlanner.FOCUS_FREE, rhythm.getNextAction());
        assertEquals("自由专注", rhythm.getCtaLabel());
        assertEquals("/pomodoro", rhythm.getCtaPath());
        assertNull(rhythm.getFocusTask());
    }

    @Test
    void rhythmAsksForDiaryAfterMainLoop() {
        stubUser();
        stubDueWords(List.of());
        stubOpenTasks(List.of());
        stubHeat(0, 0, 0);
        when(checkInRepository.existsByUserIdAndCheckInDate("u1", LocalDate.now())).thenReturn(true);
        when(diaryRepository.findByUserIdAndDiaryDate("u1", LocalDate.now().toString()))
                .thenReturn(Optional.empty());

        RhythmResponse rhythm = dashboardService.getRhythm("alice");

        assertEquals(RhythmPlanner.WRITE_DIARY, rhythm.getNextAction());
        assertEquals("写今日日记", rhythm.getCtaLabel());
        assertEquals("/diary?date=" + LocalDate.now(), rhythm.getCtaPath());
        assertEquals("主线接上了，用日记把这一天收口。", rhythm.getReason());
    }

    @Test
    void rhythmHeatMatchesPublishedWeights() {
        stubUser();
        stubDueWords(List.of());
        stubOpenTasks(List.of());
        stubHeat(2, 2, 3);
        when(checkInRepository.existsByUserIdAndCheckInDate("u1", LocalDate.now())).thenReturn(true);

        RhythmResponse rhythm = dashboardService.getRhythm("alice");

        assertEquals(1, rhythm.getHeat().getCheckIn());
        assertEquals(4, rhythm.getHeat().getPomodoro());
        assertEquals(2, rhythm.getHeat().getWord());
        assertEquals(9, rhythm.getHeat().getTask());
        assertEquals(16, rhythm.getHeat().getTotal());
        assertEquals(2, rhythm.getHeat().getPomodoroCount());
        assertEquals(2, rhythm.getHeat().getWordCount());
        assertEquals(3, rhythm.getHeat().getTaskCount());
    }

    @Test
    void statsAggregatesServiceCounts() {
        stubUser();
        when(checkInService.getConsecutiveDays("u1")).thenReturn(5);
        when(wordService.getWordCount("u1")).thenReturn(12);
        when(pomodoroService.getTotalCount("u1")).thenReturn(8);
        when(checkInService.getTotalDays("u1")).thenReturn(20);

        Map<String, Object> stats = dashboardService.getStats("alice");

        assertEquals(5, stats.get("checkInDays"));
        assertEquals(12, stats.get("wordCount"));
        assertEquals(8, stats.get("pomodoroCount"));
        assertEquals(20, stats.get("totalDays"));
    }

    @Test
    void todayTasksSummarizesCheckInWordsAndPomodoros() {
        stubUser();
        when(checkInRepository.existsByUserIdAndCheckInDate("u1", LocalDate.now())).thenReturn(true);
        stubDueWords(List.of(word("w1", "apple"), word("w2", "banana")));
        when(pomodoroRepository.countByUserIdAndTypeAndStartTimeBetween(
                eq("u1"), eq("work"), nullableArg(LocalDateTime.class), nullableArg(LocalDateTime.class)))
                .thenReturn(4L);

        Map<String, Object> tasks = dashboardService.getTodayTasks("alice");

        assertEquals(true, tasks.get("hasCheckedIn"));
        assertEquals(2, tasks.get("todayWordCount"));
        assertEquals(4L, tasks.get("todayPomodoroCount"));
        verify(pomodoroRepository).countByUserIdAndTypeAndStartTimeBetween(
                eq("u1"), eq("work"), nullableArg(LocalDateTime.class), nullableArg(LocalDateTime.class));
    }

    @Test
    void recentActivitiesSurvivesNullTimestampsAndPomodoroType() {
        stubUser();
        CheckIn checkIn = new CheckIn();
        checkIn.setCheckInDate(LocalDate.now());
        checkIn.setCreateTime(null);

        Pomodoro pomodoro = new Pomodoro();
        pomodoro.setStartTime(LocalDateTime.now());
        pomodoro.setType(null);
        pomodoro.setDuration(null);

        Word word = new Word();
        word.setWord(null);
        word.setCreateTime(null);
        word.setLastReviewTime(null);

        when(checkInRepository.findByUserIdOrderByCheckInDateDesc("u1")).thenReturn(List.of(checkIn));
        when(pomodoroRepository.findByUserIdOrderByStartTimeDesc("u1")).thenReturn(List.of(pomodoro));
        when(wordRepository.findByUserIdOrderByCreateTimeDesc("u1")).thenReturn(List.of(word));
        when(diaryRepository.findByUserIdOrderByDiaryDateDesc("u1")).thenReturn(List.of());

        List<Map<String, Object>> activities = dashboardService.getRecentActivities("alice");

        assertEquals(2, activities.size());
        assertEquals("完成打卡", activities.stream().filter(a -> "checkin".equals(a.get("type"))).findFirst().orElseThrow().get("title"));
        assertEquals("完成工作番茄钟 (0分钟)", activities.stream().filter(a -> "pomodoro".equals(a.get("type"))).findFirst().orElseThrow().get("title"));
    }

    @Test
    void statsThrowsWhenUserMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> dashboardService.getStats("ghost"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    private void stubUser() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        org.mockito.Mockito.lenient()
                .when(diaryRepository.findByUserIdAndDiaryDate(eq("u1"), nullableArg(String.class)))
                .thenReturn(Optional.empty());
    }

    private void stubDueWords(List<Word> words) {
        when(wordRepository.findByUserIdAndStatusNotAndDueDateLessThanEqualOrderByDueDateAsc(
                eq("u1"), eq("done"), nullableArg(LocalDateTime.class)
        )).thenReturn(words);
    }

    private void stubOpenTasks(List<Task> tasks) {
        when(taskService.listTasks("alice", Map.of())).thenReturn(tasks);
    }

    private void stubHeat(int pomodoro, int word, int task) {
        Map<String, Integer> day = new HashMap<>();
        day.put("pomodoro", pomodoro);
        day.put("word", word);
        day.put("task", task);
        when(calendarService.getCalendarData("alice", LocalDate.now(), LocalDate.now()))
                .thenReturn(Map.of(LocalDate.now().toString(), day));
    }

    private static Word word(String id, String text) {
        Word word = new Word();
        word.setId(id);
        word.setWord(text);
        word.setTranslation(text + "-zh");
        return word;
    }

    private static Task task(String id, String status, String title) {
        Task task = new Task();
        task.setId(id);
        task.setStatus(status);
        task.setTitle(title);
        return task;
    }
}
