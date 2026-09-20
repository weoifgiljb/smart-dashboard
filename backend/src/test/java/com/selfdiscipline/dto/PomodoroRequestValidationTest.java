package com.selfdiscipline.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PomodoroRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void durationMustBePresentAndPositive() {
        PomodoroRequest missing = new PomodoroRequest();
        Set<ConstraintViolation<PomodoroRequest>> missingViolations = validator.validate(missing);
        assertTrue(missingViolations.stream().anyMatch(v -> "duration".equals(v.getPropertyPath().toString())));

        PomodoroRequest zero = new PomodoroRequest();
        zero.setDuration(0);
        assertFalse(validator.validate(zero).isEmpty());

        PomodoroRequest ok = new PomodoroRequest();
        ok.setDuration(25);
        assertTrue(validator.validate(ok).isEmpty());
    }
}
