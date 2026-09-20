package com.selfdiscipline.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class TaskCreateRequest {
    @NotBlank(message = "标题不能为空")
    private String title;
    private String description;
    private String status;
    private String priority;
    private List<String> tags;
    private String startDate;
    private String dueDate;
    private Integer estimateMinutes;
    private Integer actualMinutes;
    private String recurrenceRule;
    private String remindAt;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getEstimateMinutes() {
        return estimateMinutes;
    }

    public void setEstimateMinutes(Integer estimateMinutes) {
        this.estimateMinutes = estimateMinutes;
    }

    public Integer getActualMinutes() {
        return actualMinutes;
    }

    public void setActualMinutes(Integer actualMinutes) {
        this.actualMinutes = actualMinutes;
    }

    public String getRecurrenceRule() {
        return recurrenceRule;
    }

    public void setRecurrenceRule(String recurrenceRule) {
        this.recurrenceRule = recurrenceRule;
    }

    public String getRemindAt() {
        return remindAt;
    }

    public void setRemindAt(String remindAt) {
        this.remindAt = remindAt;
    }
}
