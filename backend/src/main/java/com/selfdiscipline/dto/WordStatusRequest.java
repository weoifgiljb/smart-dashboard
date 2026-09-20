package com.selfdiscipline.dto;

import jakarta.validation.constraints.NotBlank;

public class WordStatusRequest {
    @NotBlank(message = "状态不能为空")
    private String status; // todo / done

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}





