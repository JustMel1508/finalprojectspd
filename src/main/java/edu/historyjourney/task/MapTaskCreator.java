package edu.historyjourney.task;
public class MapTaskCreator extends TaskCreator {
    @Override protected Task createTask(TaskDefinition d) {
        return new MapTask(d.id(), d.prompt(), java.util.List.copyOf(d.options()), d.correctOptionId(), d.hint(), d.explanation(), d.sourceId());
    }
}
