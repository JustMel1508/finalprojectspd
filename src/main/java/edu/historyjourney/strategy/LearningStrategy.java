package edu.historyjourney.strategy;
import edu.historyjourney.model.*;
import edu.historyjourney.task.Task;
public class LearningStrategy implements AnswerCheckingStrategy {
    public Attempt.Answer check(Task task, String optionId, Attempt.Answer previous) {
        if (previous != null && previous.correct()) throw GameException.conflict("Это задание уже выполнено.");
        return AnswerEvaluator.evaluate(task, optionId, previous);
    }
    public boolean canAdvance(Attempt.Answer answer) { return answer != null && answer.correct(); }
    public String feedback(Task task, Attempt.Answer answer) {
        return answer.correct() ? "Верно. " + task.explanation() : "Попробуйте ещё раз. " + task.hint();
    }
}
