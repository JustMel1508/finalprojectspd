package edu.historyjourney.strategy;
import edu.historyjourney.model.*;
import edu.historyjourney.task.Task;
public class AssessmentStrategy implements AnswerCheckingStrategy {
    public Attempt.Answer check(Task task, String optionId, Attempt.Answer previous) {
        if (previous != null) throw GameException.conflict("В проверке знаний разрешён один ответ на задание.");
        return AnswerEvaluator.evaluate(task, optionId, null);
    }
    public boolean canAdvance(Attempt.Answer answer) { return answer != null; }
    public String feedback(Task task, Attempt.Answer answer) { return "Ответ принят. Оценка появится после завершения всех заданий."; }
}
