package edu.historyjourney.task;
import java.util.List;
public record QuizTask(String id, String prompt, List<Task.Option> options, String correctOptionId,
        String hint, String explanation, String sourceId) implements Task {
    public String kind() { return "QUIZ"; }
}
