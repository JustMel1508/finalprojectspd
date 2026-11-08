package edu.historyjourney.result;
public record MapTaskResult(String taskId, String prompt, String chosen, String expected, boolean correct,
        int tries, int errors, String explanation, String sourceId, double lon, double lat) implements TaskResult {
    public void accept(ResultVisitor visitor) { visitor.visit(this); }
}
