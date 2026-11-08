package edu.historyjourney.result;
public class TextReportVisitor implements ResultVisitor {
    private final StringBuilder text = new StringBuilder();
    private void write(TaskResult r, String type) {
        text.append(type).append(": ").append(r.prompt()).append("\nВаш ответ: ").append(r.chosen())
            .append("\nПравильный ответ: ").append(r.expected()).append("\nИтог: ").append(r.correct() ? "верно" : "неверно")
            .append("; ответов: ").append(r.tries()).append("; ошибок: ").append(r.errors())
            .append("\n").append(r.explanation()).append("\nИсточник: ").append(r.sourceId()).append("\n");
    }
    public void visit(QuizResult r) { write(r, "Вопрос"); text.append("\n"); }
    public void visit(MapTaskResult r) { write(r, "Карта"); text.append("Выбранная точка: ").append(r.lon()).append(", ").append(r.lat()).append("\n\n"); }
    public String text() { return text.toString(); }
}
