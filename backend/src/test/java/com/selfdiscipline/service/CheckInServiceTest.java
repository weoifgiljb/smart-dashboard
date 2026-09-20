package com.selfdiscipline.service;

import com.selfdiscipline.exception.ApiException;
import com.selfdiscipline.model.CheckIn;
import com.selfdiscipline.model.User;
import com.selfdiscipline.repository.CheckInRepository;
import com.selfdiscipline.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

    @Mock
    private CheckInRepository checkInRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CalendarService calendarService;

    @InjectMocks
    private CheckInService checkInService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId("u1");
        alice.setUsername("alice");
    }

    @Test
    void duplicateCheckInIsConflict() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(checkInRepository.existsByUserIdAndCheckInDate("u1", LocalDate.now())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> checkInService.checkIn("alice"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("今日已打卡", ex.getMessage());
    }

    @Test
    void heatValueUsesPublishedWeights() {
        LocalDate day = LocalDate.of(2026, 9, 20);
        when(calendarService.getCalendarData("alice", day, day)).thenReturn(
                Map.of("2026-09-20", Map.of("pomodoro", 2, "word", 1, "task", 1))
        );

        int heat = checkInService.calculateDailyHeatValue("alice", day);

        assertEquals(1 + 4 + 1 + 3, heat);
    }

    @Test
    void consecutiveDaysCountsTodayAndYesterday() {
        when(checkInRepository.findByUserIdOrderByCheckInDateDesc("u1")).thenReturn(List.of(
                checkIn(LocalDate.now()),
                checkIn(LocalDate.now().minusDays(1))
        ));

        assertEquals(2, checkInService.getConsecutiveDays("u1"));
    }

    @Test
    void consecutiveDaysCountsFromYesterdayWhenTodayMissing() {
        when(checkInRepository.findByUserIdOrderByCheckInDateDesc("u1")).thenReturn(List.of(
                checkIn(LocalDate.now().minusDays(1)),
                checkIn(LocalDate.now().minusDays(2))
        ));

        assertEquals(2, checkInService.getConsecutiveDays("u1"));
    }

    @Test
    void consecutiveDaysIsZeroWhenYesterdayAlsoMissing() {
        when(checkInRepository.findByUserIdOrderByCheckInDateDesc("u1")).thenReturn(List.of(
                checkIn(LocalDate.now().minusDays(3))
        ));

        assertEquals(0, checkInService.getConsecutiveDays("u1"));
    }

    @Test
    void consecutiveDaysStopsAtGap() {
        when(checkInRepository.findByUserIdOrderByCheckInDateDesc("u1")).thenReturn(List.of(
                checkIn(LocalDate.now()),
                checkIn(LocalDate.now().minusDays(2))
        ));

        assertEquals(1, checkInService.getConsecutiveDays("u1"));
    }

    @Test
    void successfulCheckInPersistsHeat() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(checkInRepository.existsByUserIdAndCheckInDate("u1", LocalDate.now())).thenReturn(false);
        when(calendarService.getCalendarData(any(), any(), any())).thenReturn(Map.of());
        when(checkInRepository.save(any(CheckIn.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = checkInService.checkIn("alice");

        assertEquals("打卡成功", result.get("message"));
        assertEquals(LocalDate.now(), result.get("checkInDate"));
        assertEquals(1, result.get("heatValue"));
    }

    private static CheckIn checkIn(LocalDate date) {
        CheckIn checkIn = new CheckIn();
        checkIn.setUserId("u1");
        checkIn.setCheckInDate(date);
        return checkIn;
    }
}
