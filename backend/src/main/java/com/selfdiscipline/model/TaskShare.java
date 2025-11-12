package com.selfdiscipline.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "task_shares")
public class TaskShare {

    @Id
    private String id;

    @Indexed
    private String taskId;

    @Indexed
    private String targetUserId;

    // VIEW | EDIT
    private String role;

    private String grantedByUserId;
    private LocalDateTime createdAt;

    public TaskShare() {
        this.createdAt = LocalDateTime.now();
        this.role = "VIEW";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getGrantedByUserId() {
        return grantedByUserId;
    }

    public void setGrantedByUserId(String grantedByUserId) {
        this.grantedByUserId = grantedByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}



