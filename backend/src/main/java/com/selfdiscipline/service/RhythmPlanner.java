package com.selfdiscipline.service;

/**
 * Deterministic "next action" for the daily rhythm loop.
 * Priority: check-in → due words → open task focus → free focus.
 */
public final class RhythmPlanner {

    public static final String CHECK_IN = "CHECK_IN";
    public static final String REVIEW_WORDS = "REVIEW_WORDS";
    public static final String FOCUS_TASK = "FOCUS_TASK";
    public static final String FOCUS_FREE = "FOCUS_FREE";

    private RhythmPlanner() {
    }

    public static String nextAction(boolean hasCheckedIn, int dueWordCount, boolean hasOpenTask) {
        if (!hasCheckedIn) {
            return CHECK_IN;
        }
        if (dueWordCount > 0) {
            return REVIEW_WORDS;
        }
        if (hasOpenTask) {
            return FOCUS_TASK;
        }
        return FOCUS_FREE;
    }

    public static int heatTotal(int checkInScore, int pomodoroCount, int wordCount, int taskCount) {
        return checkInScore + pomodoroCount * 2 + wordCount + taskCount * 3;
    }
}
