package edu.historyjourney.task;

import java.util.HashSet;

/** Creator: common preparation calls the overridden factory method. */
public abstract class TaskCreator {
    public final Task prepare(TaskDefinition d) {
        if (d == null || blank(d.id()) || blank(d.kind()) || blank(d.prompt()) || d.options() == null || d.options().size() < 2
                || blank(d.hint()) || blank(d.explanation()) || blank(d.sourceId())) throw new IllegalArgumentException("Incomplete task");
        var ids = new HashSet<String>();
        for (var o : d.options()) {
            if (o == null || blank(o.id()) || blank(o.label()) || !ids.add(o.id())) throw new IllegalArgumentException("Duplicate/invalid option");
            if ("MAP".equals(d.kind()) && (o.lon() == null || o.lat() == null || !Double.isFinite(o.lon())
                    || !Double.isFinite(o.lat()) || o.lon() < -180 || o.lon() > 180 || o.lat() < -90 || o.lat() > 90))
                throw new IllegalArgumentException("Invalid coordinates");
        }
        if (!ids.contains(d.correctOptionId())) throw new IllegalArgumentException("Missing correct option");
        var task = createTask(d);
        if (task == null || !d.kind().equals(task.kind())) throw new IllegalArgumentException("Creator does not support this task kind");
        return task;
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    protected abstract Task createTask(TaskDefinition definition);
}
