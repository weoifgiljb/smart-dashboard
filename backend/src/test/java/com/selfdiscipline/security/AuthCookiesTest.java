package com.selfdiscipline.security;

import com.selfdiscipline.config.JwtConfig;
import com.selfdiscipline.dto.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCookiesTest {

    @Test
    void attachWritesHttpOnlySameSitePathCookies() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        JwtConfig config = new JwtConfig();
        config.setExpiration(86_400_000L);
        config.setRefreshExpiration(604_800_000L);

        AuthCookies.attach(response, new AuthResponse("access-jwt", "refresh-jwt", null), config);

        String headers = String.join("\n", response.getHeaders("Set-Cookie"));
        assertTrue(headers.contains("access_token=access-jwt"), headers);
        assertTrue(headers.contains("refresh_token=refresh-jwt"), headers);
        assertTrue(headers.contains("HttpOnly"), headers);
        assertTrue(headers.contains("Path=/"), headers);
        assertTrue(headers.contains("SameSite=Lax"), headers);
    }
}
