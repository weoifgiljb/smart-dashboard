package com.selfdiscipline.dto;

import jakarta.validation.constraints.NotBlank;

public class MemeMatchRequest {
    @NotBlank(message = "content 不能为空")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
