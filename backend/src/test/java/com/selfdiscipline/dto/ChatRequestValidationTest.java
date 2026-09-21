package com.selfdiscipline.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void conversationIdAndQuestionAreRequired() {
        ChatRequest blank = new ChatRequest();
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(blank);
        assertTrue(violations.stream().anyMatch(v -> "conversationId".equals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "question".equals(v.getPropertyPath().toString())));
    }

    @Test
    void validRequestPasses() {
        ChatRequest req = new ChatRequest();
        req.setConversationId("conv-1");
        req.setQuestion("你好");
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void replaceLastIsOptional() {
        ChatRequest req = new ChatRequest();
        req.setConversationId("conv-1");
        req.setQuestion("你好");
        req.setReplaceLast(true);
        assertTrue(validator.validate(req).isEmpty());
        assertTrue(req.getReplaceLast());
    }

    @Test
    void blankConversationIdFails() {
        ChatRequest req = new ChatRequest();
        req.setConversationId("  ");
        req.setQuestion("你好");
        assertFalse(validator.validate(req).isEmpty());
    }
}
