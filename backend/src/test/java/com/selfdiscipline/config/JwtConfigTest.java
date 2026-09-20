package com.selfdiscipline.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtConfigTest {

    @Test
    void rejectsMissingSecret() {
        JwtConfig config = new JwtConfig();
        assertThrows(IllegalStateException.class, config::afterPropertiesSet);
        config.setSecret("   ");
        assertThrows(IllegalStateException.class, config::afterPropertiesSet);
    }

    @Test
    void rejectsPublishedPlaceholderAndShortSecret() {
        JwtConfig config = new JwtConfig();
        config.setSecret(JwtConfig.INSECURE_PLACEHOLDER);
        assertThrows(IllegalStateException.class, config::afterPropertiesSet);

        config.setSecret("too-short-to-be-hmac-sha-key");
        assertThrows(IllegalStateException.class, config::afterPropertiesSet);
    }

    @Test
    void acceptsLongRandomSecret() {
        JwtConfig config = new JwtConfig();
        config.setSecret("unit-test-secret-key-must-be-32b!!");
        assertDoesNotThrow(config::afterPropertiesSet);
    }
}
