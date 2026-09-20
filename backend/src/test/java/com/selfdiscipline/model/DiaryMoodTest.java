package com.selfdiscipline.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiaryMoodTest {

    @Test
    void unknownOrBlankFallsBackToNeutral() {
        assertEquals("neutral", DiaryMood.normalize(null));
        assertEquals("neutral", DiaryMood.normalize(" "));
        assertEquals("neutral", DiaryMood.normalize("angry"));
    }

    @Test
    void acceptsCodeAndEnumName() {
        assertEquals("happy", DiaryMood.normalize("happy"));
        assertEquals("happy", DiaryMood.normalize("HAPPY"));
        assertEquals("energetic", DiaryMood.normalize("Energetic"));
    }
}
