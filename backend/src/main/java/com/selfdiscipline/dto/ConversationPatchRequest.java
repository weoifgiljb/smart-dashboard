package com.selfdiscipline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ConversationPatchRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 40, message = "标题最多 40 字")
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
