package com.selfdiscipline.controller;

import com.selfdiscipline.dto.AuthResponse;
import com.selfdiscipline.dto.LoginRequest;
import com.selfdiscipline.dto.RefreshTokenRequest;
import com.selfdiscipline.dto.RegisterRequest;
import com.selfdiscipline.dto.UserPublicDto;
import com.selfdiscipline.config.JwtConfig;
import com.selfdiscipline.security.AuthCookies;
import com.selfdiscipline.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtConfig jwtConfig;

    public AuthController(AuthService authService, JwtConfig jwtConfig) {
        this.authService = authService;
        this.jwtConfig = jwtConfig;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        AuthResponse tokens = authService.register(request);
        AuthCookies.attach(response, tokens, jwtConfig);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthResponse tokens = authService.login(request);
        AuthCookies.attach(response, tokens, jwtConfig);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader(value = "Refresh-Token", required = false) String refreshHeader,
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String token = refreshHeader;
        if ((token == null || token.isBlank()) && body != null) {
            token = body.getRefreshToken();
        }
        if (token == null || token.isBlank()) {
            token = AuthCookies.read(request, AuthCookies.REFRESH);
        }
        AuthResponse tokens = authService.refresh(token);
        AuthCookies.attach(response, tokens, jwtConfig);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        AuthCookies.clear(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserPublicDto> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(authService.getCurrentUserPublic(authentication.getName()));
    }
}
