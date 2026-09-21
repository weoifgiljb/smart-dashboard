package com.selfdiscipline.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatRequest {
    @NotBlank(message = "会话不能为空")
    private String conversationId;

    @NotBlank(message = "问题不能为空")
    private String question;

    private boolean replaceLast;

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public boolean isReplaceLast() {
        return replaceLast;
    }

    public boolean getReplaceLast() {
        return replaceLast;
    }

    public void setReplaceLast(boolean replaceLast) {
        this.replaceLast = replaceLast;
    }
}

