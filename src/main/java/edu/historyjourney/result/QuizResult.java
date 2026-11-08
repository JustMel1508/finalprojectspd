package edu.historyjourney.result;
public record QuizResult(String taskId, String prompt, String chosen, String expected, boolean correct,
        int tries, int errors, String explanation, String sourceId) implements TaskResult {
    public void accept(ResultVisitor visitor) { visitor.visit(this); }
}
