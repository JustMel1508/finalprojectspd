package edu.historyjourney.task;
public class QuizTaskCreator extends TaskCreator {
    @Override protected Task createTask(TaskDefinition d) {
        return new QuizTask(d.id(), d.prompt(), java.util.List.copyOf(d.options()), d.correctOptionId(), d.hint(), d.explanation(), d.sourceId());
    }
}
