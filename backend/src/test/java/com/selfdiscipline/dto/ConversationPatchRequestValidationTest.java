package com.selfdiscipline.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationPatchRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void titleIsRequired() {
        ConversationPatchRequest blank = new ConversationPatchRequest();
        Set<ConstraintViolation<ConversationPatchRequest>> violations = validator.validate(blank);
        assertTrue(violations.stream().anyMatch(v -> "title".equals(v.getPropertyPath().toString())));
    }

    @Test
    void titleLongerThan40Fails() {
        ConversationPatchRequest req = new ConversationPatchRequest();
        req.setTitle("12345678901234567890123456789012345678901");
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void validTitlePasses() {
        ConversationPatchRequest req = new ConversationPatchRequest();
        req.setTitle("日程规划");
        assertTrue(validator.validate(req).isEmpty());
    }
}
