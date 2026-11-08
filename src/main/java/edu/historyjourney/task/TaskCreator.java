package edu.historyjourney.task;

import java.util.HashSet;

/** Creator: common preparation calls the overridden factory method. */
public abstract class TaskCreator {
    public final Task prepare(TaskDefinition d) {
        if (d == null || d.id() == null || d.prompt() == null || d.options() == null || d.options().size() < 2
                || d.hint() == null || d.explanation() == null || d.sourceId() == null) throw new IllegalArgumentException("Incomplete task");
        var ids = new HashSet<String>();
        for (var o : d.options()) {
            if (o.id() == null || o.label() == null || !ids.add(o.id())) throw new IllegalArgumentException("Duplicate/invalid option");
            if ("MAP".equals(d.kind()) && (o.lon() == null || o.lat() == null || !Double.isFinite(o.lon())
                    || !Double.isFinite(o.lat()) || o.lon() < -180 || o.lon() > 180 || o.lat() < -90 || o.lat() > 90))
                throw new IllegalArgumentException("Invalid coordinates");
        }
        if (!ids.contains(d.correctOptionId())) throw new IllegalArgumentException("Missing correct option");
        return createTask(d);
    }
    protected abstract Task createTask(TaskDefinition definition);
}
