package com.selfdiscipline.dto;

import com.selfdiscipline.model.User;

import java.time.LocalDateTime;

public class UserPublicDto {
    private String id;
    private String username;
    private String email;
    private LocalDateTime createTime;

    public UserPublicDto() {
    }

    public UserPublicDto(String id, String username, String email, LocalDateTime createTime) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.createTime = createTime;
    }

    public static UserPublicDto from(User user) {
        if (user == null) {
            return null;
        }
        return new UserPublicDto(user.getId(), user.getUsername(), user.getEmail(), user.getCreateTime());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
