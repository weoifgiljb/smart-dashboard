package com.selfdiscipline.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequest {
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
