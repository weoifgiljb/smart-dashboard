package com.selfdiscipline.util;

import com.selfdiscipline.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtConfig jwtConfig;

    public JwtUtil(JwtConfig jwtConfig) {
        JwtConfig.validateSecret(jwtConfig.getSecret());
        this.jwtConfig = jwtConfig;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        return generateToken(username, TYPE_ACCESS, jwtConfig.getExpiration());
    }

    public String generateRefreshToken(String username) {
        long ttl = jwtConfig.getRefreshExpiration() != null ? jwtConfig.getRefreshExpiration() : 604800000L;
        return generateToken(username, TYPE_REFRESH, ttl);
    }

    private String generateToken(String username, String type, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("typ", type)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getTokenType(String token) {
        Object typ = parseClaims(token).get("typ");
        return typ == null ? TYPE_ACCESS : typ.toString();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateAccessToken(String token) {
        try {
            return TYPE_ACCESS.equals(getTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            return TYPE_REFRESH.equals(getTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
