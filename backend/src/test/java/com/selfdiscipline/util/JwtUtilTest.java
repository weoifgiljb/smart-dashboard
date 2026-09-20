package com.selfdiscipline.util;

import com.selfdiscipline.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret("test-secret-key-must-be-at-least-32b");
        config.setExpiration(3600000L);
        config.setRefreshExpiration(86400000L);
        jwtUtil = new JwtUtil(config);
    }

    @Test
    void accessTokenIsNotAcceptedAsRefresh() {
        String access = jwtUtil.generateToken("alice");
        assertTrue(jwtUtil.validateAccessToken(access));
        assertFalse(jwtUtil.validateRefreshToken(access));
        assertEquals("alice", jwtUtil.getUsernameFromToken(access));
    }

    @Test
    void refreshTokenIsNotAcceptedAsAccess() {
        String refresh = jwtUtil.generateRefreshToken("alice");
        assertTrue(jwtUtil.validateRefreshToken(refresh));
        assertFalse(jwtUtil.validateAccessToken(refresh));
    }

    @Test
    void invalidTokenFailsValidation() {
        assertFalse(jwtUtil.validateToken("not-a-jwt"));
        assertFalse(jwtUtil.validateAccessToken("not-a-jwt"));
    }

    @Test
    void shortOrPlaceholderSecretIsRejected() {
        JwtConfig shortSecret = new JwtConfig();
        shortSecret.setSecret("too-short");
        shortSecret.setExpiration(3600000L);
        assertThrows(IllegalStateException.class, () -> new JwtUtil(shortSecret));

        JwtConfig placeholder = new JwtConfig();
        placeholder.setSecret(JwtConfig.INSECURE_PLACEHOLDER);
        placeholder.setExpiration(3600000L);
        assertThrows(IllegalStateException.class, () -> new JwtUtil(placeholder));
    }
}
