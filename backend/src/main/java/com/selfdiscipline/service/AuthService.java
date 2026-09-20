package com.selfdiscipline.service;

import com.selfdiscipline.dto.AuthResponse;
import com.selfdiscipline.dto.LoginRequest;
import com.selfdiscipline.dto.RegisterRequest;
import com.selfdiscipline.dto.UserPublicDto;
import com.selfdiscipline.exception.ApiException;
import com.selfdiscipline.model.User;
import com.selfdiscipline.repository.UserRepository;
import com.selfdiscipline.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw ApiException.conflict("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());

        user = userRepository.save(user);
        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> ApiException.unauthorized("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw ApiException.unauthorized("用户名或密码错误");
        }

        return issueTokens(user);
    }

    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || !jwtUtil.validateRefreshToken(refreshToken)) {
            throw ApiException.unauthorized("刷新令牌无效或已过期");
        }
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        User user = getCurrentUser(username);
        return issueTokens(user);
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("用户不存在"));
    }

    public UserPublicDto getCurrentUserPublic(String username) {
        return UserPublicDto.from(getCurrentUser(username));
    }

    private AuthResponse issueTokens(User user) {
        String access = jwtUtil.generateToken(user.getUsername());
        String refresh = jwtUtil.generateRefreshToken(user.getUsername());
        return new AuthResponse(access, refresh, UserPublicDto.from(user));
    }
}
