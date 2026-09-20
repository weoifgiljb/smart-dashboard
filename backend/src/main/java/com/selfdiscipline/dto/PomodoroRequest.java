package com.selfdiscipline.dto;

import jakarta.validation.constraints.Min;

public class PomodoroRequest {
    @Min(1)
    private Integer duration;
    private String type;
    private String taskId;

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}



