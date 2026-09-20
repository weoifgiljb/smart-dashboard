package com.selfdiscipline.model;

public enum DiaryMood {
    HAPPY("happy"),
    NEUTRAL("neutral"),
    SAD("sad"),
    ENERGETIC("energetic"),
    TIRED("tired");

    private final String code;

    DiaryMood(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return NEUTRAL.code;
        }
        String value = raw.trim();
        for (DiaryMood mood : values()) {
            if (mood.code.equalsIgnoreCase(value) || mood.name().equalsIgnoreCase(value)) {
                return mood.code;
            }
        }
        return NEUTRAL.code;
    }
}
