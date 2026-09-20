package com.selfdiscipline.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void passwordMustBeAtLeastEightCharacters() {
        RegisterRequest shortPassword = validExceptPassword("short");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(shortPassword);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "password".equals(v.getPropertyPath().toString())));

        RegisterRequest ok = validExceptPassword("password");
        assertTrue(validator.validate(ok).isEmpty());
    }

    private static RegisterRequest validExceptPassword(String password) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("bob");
        req.setEmail("b@example.com");
        req.setPassword(password);
        return req;
    }
}
