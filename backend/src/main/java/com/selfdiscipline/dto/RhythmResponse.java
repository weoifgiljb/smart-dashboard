package com.selfdiscipline.dto;

import java.util.ArrayList;
import java.util.List;

public class RhythmResponse {
    private String nextAction;
    private String reason;
    private String ctaLabel;
    private String ctaPath;
    private boolean hasCheckedIn;
    private int dueWordCount;
    private Heat heat = new Heat();
    private List<DueWord> dueWords = new ArrayList<>();
    private FocusTask focusTask;

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCtaLabel() {
        return ctaLabel;
    }

    public void setCtaLabel(String ctaLabel) {
        this.ctaLabel = ctaLabel;
    }

    public String getCtaPath() {
        return ctaPath;
    }

    public void setCtaPath(String ctaPath) {
        this.ctaPath = ctaPath;
    }

    public boolean isHasCheckedIn() {
        return hasCheckedIn;
    }

    public void setHasCheckedIn(boolean hasCheckedIn) {
        this.hasCheckedIn = hasCheckedIn;
    }

    public int getDueWordCount() {
        return dueWordCount;
    }

    public void setDueWordCount(int dueWordCount) {
        this.dueWordCount = dueWordCount;
    }

    public Heat getHeat() {
        return heat;
    }

    public void setHeat(Heat heat) {
        this.heat = heat;
    }

    public List<DueWord> getDueWords() {
        return dueWords;
    }

    public void setDueWords(List<DueWord> dueWords) {
        this.dueWords = dueWords;
    }

    public FocusTask getFocusTask() {
        return focusTask;
    }

    public void setFocusTask(FocusTask focusTask) {
        this.focusTask = focusTask;
    }

    public static class Heat {
        private int checkIn;
        private int pomodoro;
        private int word;
        private int task;
        private int total;
        private int pomodoroCount;
        private int wordCount;
        private int taskCount;

        public int getCheckIn() {
            return checkIn;
        }

        public void setCheckIn(int checkIn) {
            this.checkIn = checkIn;
        }

        public int getPomodoro() {
            return pomodoro;
        }

        public void setPomodoro(int pomodoro) {
            this.pomodoro = pomodoro;
        }

        public int getWord() {
            return word;
        }

        public void setWord(int word) {
            this.word = word;
        }

        public int getTask() {
            return task;
        }

        public void setTask(int task) {
            this.task = task;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getPomodoroCount() {
            return pomodoroCount;
        }

        public void setPomodoroCount(int pomodoroCount) {
            this.pomodoroCount = pomodoroCount;
        }

        public int getWordCount() {
            return wordCount;
        }

        public void setWordCount(int wordCount) {
            this.wordCount = wordCount;
        }

        public int getTaskCount() {
            return taskCount;
        }

        public void setTaskCount(int taskCount) {
            this.taskCount = taskCount;
        }
    }

    public static class DueWord {
        private String id;
        private String word;
        private String translation;

        public DueWord() {
        }

        public DueWord(String id, String word, String translation) {
            this.id = id;
            this.word = word;
            this.translation = translation;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

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
    }

    public static class FocusTask {
        private String id;
        private String title;

        public FocusTask() {
        }

        public FocusTask(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
