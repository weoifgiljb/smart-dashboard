package com.selfdiscipline.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RhythmPlannerTest {

    @Test
    void prefersCheckInWhenNotCheckedIn() {
        assertEquals(RhythmPlanner.CHECK_IN, RhythmPlanner.nextAction(false, 12, true));
    }

    @Test
    void prefersDueWordsAfterCheckIn() {
        assertEquals(RhythmPlanner.REVIEW_WORDS, RhythmPlanner.nextAction(true, 3, true));
    }

    @Test
    void prefersTaskFocusWhenNoDueWords() {
        assertEquals(RhythmPlanner.FOCUS_TASK, RhythmPlanner.nextAction(true, 0, true));
    }

    @Test
    void fallsBackToFreeFocus() {
        assertEquals(RhythmPlanner.FOCUS_FREE, RhythmPlanner.nextAction(true, 0, false, true));
    }

    @Test
    void prefersDiaryAfterMainLoopWhenTodayHasNoEntry() {
        assertEquals(RhythmPlanner.WRITE_DIARY, RhythmPlanner.nextAction(true, 0, false, false));
    }

    @Test
    void doesNotInsertDiaryBeforeCheckIn() {
        assertEquals(RhythmPlanner.CHECK_IN, RhythmPlanner.nextAction(false, 0, false, false));
    }

    @Test
    void heatMatchesPublishedWeights() {
        assertEquals(1 + 4 + 2 + 9, RhythmPlanner.heatTotal(1, 2, 2, 3));
        assertEquals(0, RhythmPlanner.heatTotal(0, 0, 0, 0));
    }
}
