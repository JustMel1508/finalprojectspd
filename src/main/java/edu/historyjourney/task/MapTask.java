package edu.historyjourney.task;
import java.util.List;
public record MapTask(String id, String prompt, List<Task.Option> options, String correctOptionId,
        String hint, String explanation, String sourceId) implements Task {
    public String kind() { return "MAP"; }
}
