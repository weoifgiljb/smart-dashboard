package com.selfdiscipline.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig implements InitializingBean {
    public static final String INSECURE_PLACEHOLDER = "change-me-in-production-use-at-least-32-chars!!";
    public static final int MIN_SECRET_BYTES = 32;

    private String secret;
    private Long expiration;
    private Long refreshExpiration = 604800000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }

    public Long getRefreshExpiration() {
        return refreshExpiration;
    }

    public void setRefreshExpiration(Long refreshExpiration) {
        this.refreshExpiration = refreshExpiration;
    }

    @Override
    public void afterPropertiesSet() {
        validateSecret(secret);
    }

    public static void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET 未配置，拒绝启动");
        }
        if (INSECURE_PLACEHOLDER.equals(secret)) {
            throw new IllegalStateException("JWT_SECRET 不能使用仓库内置占位密钥");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET 至少需要 32 字节");
        }
    }
}
