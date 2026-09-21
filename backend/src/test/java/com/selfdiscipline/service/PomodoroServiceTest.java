package com.selfdiscipline.service;

import com.selfdiscipline.dto.PomodoroRequest;
import com.selfdiscipline.exception.ApiException;
import com.selfdiscipline.model.Pomodoro;
import com.selfdiscipline.model.User;
import com.selfdiscipline.repository.PomodoroRepository;
import com.selfdiscipline.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static com.selfdiscipline.testsupport.MockitoArgs.nullableArg;
import static com.selfdiscipline.testsupport.MockitoArgs.stubSaveReturnsArg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PomodoroServiceTest {

    @Mock
    private PomodoroRepository pomodoroRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TaskService taskService;

    @InjectMocks
    private PomodoroService pomodoroService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId("u1");
        alice.setUsername("alice");
    }

    @Test
    void startThrowsWhenUserMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class,
                () -> pomodoroService.startPomodoro("ghost", request(25, "work", "t1")));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void workPomodoroWithTaskWritesActualMinutes() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        stubSaveReturnsArg(pomodoroRepository, Pomodoro.class);

        Pomodoro saved = pomodoroService.startPomodoro("alice", request(25, "work", "t1"));

        assertEquals("u1", saved.getUserId());
        assertEquals(25, saved.getDuration());
        assertEquals("work", saved.getType());
        assertEquals("t1", saved.getTaskId());
        assertEquals(saved.getStartTime().plusMinutes(25), saved.getEndTime());
        verify(taskService).addActualMinutes("alice", "t1", 25);
    }

    @Test
    void breakPomodoroDoesNotWriteTaskMinutes() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        stubSaveReturnsArg(pomodoroRepository, Pomodoro.class);

        pomodoroService.startPomodoro("alice", request(5, "break", "t1"));

        verify(taskService, never()).addActualMinutes(nullableArg(String.class), nullableArg(String.class), anyInt());
    }

    @Test
    void workPomodoroWithoutTaskDoesNotWriteMinutes() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        stubSaveReturnsArg(pomodoroRepository, Pomodoro.class);

        pomodoroService.startPomodoro("alice", request(25, "work", null));

        verify(taskService, never()).addActualMinutes(nullableArg(String.class), nullableArg(String.class), anyInt());
    }

    @Test
    void statsReturnsTodayAndTotalWorkCounts() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(pomodoroRepository.countByUserIdAndTypeAndStartTimeBetween(
                eq("u1"), eq("work"), nullableArg(LocalDateTime.class), nullableArg(LocalDateTime.class)))
                .thenReturn(3L);
        when(pomodoroRepository.countByUserIdAndType("u1", "work")).thenReturn(10L);

        Map<String, Object> stats = pomodoroService.getStats("alice");

        assertEquals(3L, stats.get("todayCount"));
        assertEquals(10L, stats.get("totalCount"));
        verify(pomodoroRepository).countByUserIdAndTypeAndStartTimeBetween(
                eq("u1"), eq("work"), nullableArg(LocalDateTime.class), nullableArg(LocalDateTime.class));
        verify(pomodoroRepository).countByUserIdAndType("u1", "work");
    }

    private static PomodoroRequest request(int duration, String type, String taskId) {
        PomodoroRequest req = new PomodoroRequest();
        req.setDuration(duration);
        req.setType(type);
        req.setTaskId(taskId);
        return req;
    }
}
