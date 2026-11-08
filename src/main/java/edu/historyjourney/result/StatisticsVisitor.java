package edu.historyjourney.result;
public class StatisticsVisitor implements ResultVisitor {
    private int tasks, correct, tries, errors, mapCorrect;
    private void count(TaskResult r) { tasks++; if (r.correct()) correct++; tries += r.tries(); errors += r.errors(); }
    public void visit(QuizResult r) { count(r); }
    public void visit(MapTaskResult r) { count(r); if (r.correct()) mapCorrect++; }
    public Statistics summary() { return new Statistics(tasks, correct, tries, errors, mapCorrect); }
    public record Statistics(int tasks, int correct, int tries, int errors, int mapCorrect) {}
}
