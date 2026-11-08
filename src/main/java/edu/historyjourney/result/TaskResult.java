package edu.historyjourney.result;
public interface TaskResult {
    String taskId(); String prompt(); String chosen(); String expected(); boolean correct();
    int tries(); int errors(); String explanation(); String sourceId();
    void accept(ResultVisitor visitor);
}
