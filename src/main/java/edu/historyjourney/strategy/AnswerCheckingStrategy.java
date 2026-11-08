package edu.historyjourney.strategy;
import edu.historyjourney.model.Attempt;
import edu.historyjourney.task.Task;
public interface AnswerCheckingStrategy {
    Attempt.Answer check(Task task, String optionId, Attempt.Answer previous);
    boolean canAdvance(Attempt.Answer answer);
    String feedback(Task task, Attempt.Answer answer);
}
