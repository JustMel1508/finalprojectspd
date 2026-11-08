package edu.historyjourney.task;

import java.util.List;

public interface Task {
    String id(); String prompt(); List<Option> options(); String correctOptionId();
    String hint(); String explanation(); String sourceId(); String kind();
    default boolean isCorrect(String optionId) { return correctOptionId().equals(optionId); }
    record Option(String id, String label, Double lon, Double lat) {}
}
