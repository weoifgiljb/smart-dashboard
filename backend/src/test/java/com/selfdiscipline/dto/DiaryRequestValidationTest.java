package com.selfdiscipline.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiaryRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void contentAndDateAreRequired() {
        DiaryRequest blank = new DiaryRequest();
        Set<ConstraintViolation<DiaryRequest>> violations = validator.validate(blank);
        assertTrue(violations.stream().anyMatch(v -> "content".equals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "diaryDate".equals(v.getPropertyPath().toString())));
    }

    @Test
    void diaryDateMustBeIsoFormat() {
        DiaryRequest req = valid("hello", "2026/09/20");
        Set<ConstraintViolation<DiaryRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "diaryDate".equals(v.getPropertyPath().toString())));
    }

    @Test
    void validRequestPasses() {
        assertTrue(validator.validate(valid("hello", "2026-09-20")).isEmpty());
    }

    private static DiaryRequest valid(String content, String diaryDate) {
        DiaryRequest req = new DiaryRequest();
        req.setContent(content);
        req.setDiaryDate(diaryDate);
        req.setMood("happy");
        return req;
    }
}
