package com.selfdiscipline.dto;

import com.selfdiscipline.exception.ApiException;

public enum WordReviewResult {
    UNKNOWN,
    VAGUE,
    KNOWN;

    public static WordReviewResult from(String raw) {
        if (raw == null || raw.isBlank()) {
            return KNOWN;
        }
        return switch (raw.trim().toLowerCase()) {
            case "unknown" -> UNKNOWN;
            case "vague" -> VAGUE;
            case "known" -> KNOWN;
            default -> throw ApiException.badRequest("复习结果只能是 known、vague 或 unknown");
        };
    }
}
