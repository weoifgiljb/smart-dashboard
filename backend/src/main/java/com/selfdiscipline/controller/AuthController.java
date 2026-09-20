package com.selfdiscipline.controller;

import com.selfdiscipline.dto.AuthResponse;
import com.selfdiscipline.dto.LoginRequest;
import com.selfdiscipline.dto.RefreshTokenRequest;
import com.selfdiscipline.dto.RegisterRequest;
import com.selfdiscipline.dto.UserPublicDto;
import com.selfdiscipline.service.AuthService;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader(value = "Refresh-Token", required = false) String refreshHeader,
            @RequestBody(required = false) RefreshTokenRequest body) {
        String token = refreshHeader;
        if ((token == null || token.isBlank()) && body != null) {
            token = body.getRefreshToken();
        }
        return ResponseEntity.ok(authService.refresh(token));
    }

    @GetMapping("/me")
    public ResponseEntity<UserPublicDto> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(authService.getCurrentUserPublic(authentication.getName()));
    }
}
