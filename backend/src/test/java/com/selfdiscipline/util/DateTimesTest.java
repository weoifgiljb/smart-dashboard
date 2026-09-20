package com.selfdiscipline.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateTimesTest {

    @Test
    void parsesDateOnly() {
        LocalDateTime value = DateTimes.parseFlexible("2026-01-02");
        assertEquals(LocalDateTime.of(2026, 1, 2, 0, 0), value);
    }

    @Test
    void parsesDateTime() {
        LocalDateTime value = DateTimes.parseFlexible("2026-01-02T08:30:00");
        assertEquals(LocalDateTime.of(2026, 1, 2, 8, 30, 0), value);
    }

    @Test
    void blankIsNull() {
        assertNull(DateTimes.parseFlexible(" "));
        assertNull(DateTimes.parseFlexible(null));
    }

    @Test
    void invalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> DateTimes.parseFlexible("not-a-date"));
    }
}
