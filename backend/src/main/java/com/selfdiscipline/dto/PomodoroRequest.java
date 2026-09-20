package com.selfdiscipline.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PomodoroRequest {
    @NotNull(message = "专注时长不能为空")
    @Min(value = 1, message = "专注时长至少为 1 分钟")
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



