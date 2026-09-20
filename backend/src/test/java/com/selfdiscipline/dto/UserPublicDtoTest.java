package com.selfdiscipline.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.selfdiscipline.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPublicDtoTest {

    @Test
    void publicDtoDoesNotExposePassword() throws Exception {
        User user = new User();
        user.setId("1");
        user.setUsername("alice");
        user.setEmail("a@example.com");
        user.setPassword("$2a$10$hashed-secret");

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        String json = mapper.writeValueAsString(UserPublicDto.from(user));
        assertFalse(json.contains("password"));
        assertFalse(json.contains("hashed-secret"));
        assertTrue(json.contains("alice"));
    }

    @Test
    void userEntityIgnoresPasswordOnSerialize() throws Exception {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("$2a$10$hashed-secret");
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        String json = mapper.writeValueAsString(user);
        assertFalse(json.contains("password"));
        assertFalse(json.contains("hashed-secret"));
    }
}
