package com.selfdiscipline.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String token;
    private String refreshToken;
    private UserPublicDto user;

    public AuthResponse() {
    }

    public AuthResponse(String token, String refreshToken, UserPublicDto user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UserPublicDto getUser() {
        return user;
    }

    public void setUser(UserPublicDto user) {
        this.user = user;
    }
}
