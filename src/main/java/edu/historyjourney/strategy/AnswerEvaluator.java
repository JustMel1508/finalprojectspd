package edu.historyjourney.strategy;
import edu.historyjourney.model.Attempt;
import edu.historyjourney.task.Task;
public final class AnswerEvaluator {
    private AnswerEvaluator() {}
    public static Attempt.Answer evaluate(Task task, String optionId, Attempt.Answer previous) {
        boolean correct = task.isCorrect(optionId);
        return new Attempt.Answer(task.id(), optionId, correct, previous == null ? 1 : previous.tries()+1,
                (previous == null ? 0 : previous.errors()) + (correct ? 0 : 1));
    }
}
