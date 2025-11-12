package com.selfdiscipline.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "checkins")
public class CheckIn {
    @Id
    private String id;
    private String userId;
    private LocalDate checkInDate;
    private LocalDateTime createTime;
    private int heatValue = 0; // 热力值：当日完成的番茄、单词、任务等活动计入

    public CheckIn() {
        this.createTime = LocalDateTime.now();
        this.checkInDate = LocalDate.now();
        this.heatValue = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public int getHeatValue() {
        return heatValue;
    }

    public void setHeatValue(int heatValue) {
        this.heatValue = heatValue;
    }
}



