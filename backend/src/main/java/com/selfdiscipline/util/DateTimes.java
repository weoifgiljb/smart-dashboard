package com.selfdiscipline.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateTimes {
    private DateTimes() {
    }

    public static LocalDateTime parseFlexible(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace(' ', 'T');
        try {
            if (normalized.length() == 10) {
                return LocalDate.parse(normalized).atStartOfDay();
            }
            if (normalized.length() == 16) {
                return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            }
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("无效的日期时间: " + value);
        }
    }
}
