package com.selfdiscipline.security;

import com.selfdiscipline.config.JwtConfig;
import com.selfdiscipline.dto.AuthResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class AuthCookies {
    public static final String ACCESS = "access_token";
    public static final String REFRESH = "refresh_token";

    private AuthCookies() {
    }

    public static void attach(HttpServletResponse response, AuthResponse tokens, JwtConfig jwtConfig) {
        long accessMaxAge = seconds(jwtConfig.getExpiration(), 86400L);
        long refreshMaxAge = seconds(jwtConfig.getRefreshExpiration(), 604800L);
        add(response, ACCESS, tokens.getToken(), accessMaxAge);
        add(response, REFRESH, tokens.getRefreshToken(), refreshMaxAge);
    }

    public static void clear(HttpServletResponse response) {
        add(response, ACCESS, "", 0);
        add(response, REFRESH, "", 0);
    }

    public static String read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                String value = cookie.getValue();
                return value == null || value.isBlank() ? null : value;
            }
        }
        return null;
    }

    private static void add(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value == null ? "" : value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) Math.min(Integer.MAX_VALUE, Math.max(0, maxAgeSeconds)));
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private static long seconds(Long millis, long fallbackSeconds) {
        if (millis == null || millis <= 0) {
            return fallbackSeconds;
        }
        return Math.max(1L, millis / 1000L);
    }
}
