package edu.historyjourney.task;
import java.util.List;
public record TaskDefinition(String kind, String id, String prompt, List<Task.Option> options,
        String correctOptionId, String hint, String explanation, String sourceId) {}
