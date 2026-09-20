package com.selfdiscipline.service;

import com.selfdiscipline.config.JwtConfig;
import com.selfdiscipline.dto.AuthResponse;
import com.selfdiscipline.dto.LoginRequest;
import com.selfdiscipline.dto.RegisterRequest;
import com.selfdiscipline.exception.ApiException;
import com.selfdiscipline.model.User;
import com.selfdiscipline.repository.UserRepository;
import com.selfdiscipline.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthService authService;
    private JwtUtil jwtUtil;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret("test-secret-key-must-be-at-least-32b");
        config.setExpiration(3600000L);
        config.setRefreshExpiration(86400000L);
        jwtUtil = new JwtUtil(config);
        authService = new AuthService(userRepository, encoder, jwtUtil);
    }

    @Test
    void loginReturnsTokensWithoutPassword() {
        User stored = new User();
        stored.setId("u1");
        stored.setUsername("alice");
        stored.setEmail("a@example.com");
        stored.setPassword(encoder.encode("secret"));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(stored));

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("secret");

        AuthResponse res = authService.login(req);
        assertNotNull(res.getToken());
        assertNotNull(res.getRefreshToken());
        assertEquals("alice", res.getUser().getUsername());
        assertEquals("a@example.com", res.getUser().getEmail());
        assertTrueJwt(res);
    }

    private void assertTrueJwt(AuthResponse res) {
        assertTrue(jwtUtil.validateAccessToken(res.getToken()));
        assertTrue(jwtUtil.validateRefreshToken(res.getRefreshToken()));
    }

    @Test
    void wrongPasswordIsUnauthorized() {
        User stored = new User();
        stored.setUsername("alice");
        stored.setPassword(encoder.encode("secret"));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(stored));

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("nope");

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(req));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void duplicateUsernameIsConflict() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword("secret");
        req.setEmail("a@example.com");
        ApiException ex = assertThrows(ApiException.class, () -> authService.register(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void registerPersistsAndReturnsTokens() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("b@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("u2");
            return u;
        });

        RegisterRequest req = new RegisterRequest();
        req.setUsername("bob");
        req.setPassword("secret");
        req.setEmail("b@example.com");
        AuthResponse res = authService.register(req);
        assertEquals("bob", res.getUser().getUsername());
        assertNotNull(res.getRefreshToken());
    }

    @Test
    void refreshRejectsAccessToken() {
        String access = jwtUtil.generateToken("alice");
        ApiException ex = assertThrows(ApiException.class, () -> authService.refresh(access));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void refreshRotatesTokens() {
        User stored = new User();
        stored.setId("u1");
        stored.setUsername("alice");
        stored.setEmail("a@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(stored));
        String refresh = jwtUtil.generateRefreshToken("alice");
        AuthResponse res = authService.refresh(refresh);
        assertTrue(jwtUtil.validateAccessToken(res.getToken()));
        assertTrue(jwtUtil.validateRefreshToken(res.getRefreshToken()));
    }

    private static void assertTrue(boolean value) {
        org.junit.jupiter.api.Assertions.assertTrue(value);
    }
}
