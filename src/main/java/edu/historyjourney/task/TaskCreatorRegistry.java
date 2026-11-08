package edu.historyjourney.task;
import java.util.Map;
public class TaskCreatorRegistry {
    private final Map<String, TaskCreator> creators = Map.of("QUIZ", new QuizTaskCreator(), "MAP", new MapTaskCreator());
    public Task prepare(TaskDefinition d) {
        if (d == null || d.kind() == null) throw new IllegalArgumentException("Missing task kind");
        var creator = creators.get(d.kind());
        if (creator == null) throw new IllegalArgumentException("Unknown task kind: " + d.kind());
        return creator.prepare(d);
    }
}
