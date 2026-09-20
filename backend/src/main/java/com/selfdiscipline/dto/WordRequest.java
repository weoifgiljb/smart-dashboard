package com.selfdiscipline.dto;

import jakarta.validation.constraints.NotBlank;

public class WordRequest {
    @NotBlank(message = "单词不能为空")
    private String word;
    private String translation;
    private String example;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }
}



