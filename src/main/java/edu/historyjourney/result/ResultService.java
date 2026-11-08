package edu.historyjourney.result;

import edu.historyjourney.model.*;
import java.time.Duration;
import java.util.List;

public class ResultService {
    public List<TaskResult> results(Chapter chapter, Attempt attempt) {
        return chapter.tasks().stream().map(task -> {
            var a = attempt.answers.get(task.id());
            var chosen = task.options().stream().filter(o -> o.id().equals(a.optionId())).findFirst().orElseThrow();
            var expected = task.options().stream().filter(o -> o.id().equals(task.correctOptionId())).findFirst().orElseThrow().label();
            if ("MAP".equals(task.kind())) return (TaskResult)new MapTaskResult(task.id(), task.prompt(), chosen.label(), expected, a.correct(), a.tries(), a.errors(), task.explanation(), task.sourceId(), chosen.lon(), chosen.lat());
            return (TaskResult)new QuizResult(task.id(), task.prompt(), chosen.label(), expected, a.correct(), a.tries(), a.errors(), task.explanation(), task.sourceId());
        }).toList();
    }
    public ResultView view(Chapter chapter, Attempt a) {
        var details = results(chapter, a);
        var visitor = new StatisticsVisitor(); details.forEach(r -> r.accept(visitor));
        return new ResultView(a.passed, a.mode, a.startedAt.toString(), a.endedAt.toString(), Duration.between(a.startedAt,a.endedAt).toSeconds(), visitor.summary(), details);
    }
    public record ResultView(boolean passed, Attempt.Mode mode, String startedAt, String endedAt, long seconds,
            StatisticsVisitor.Statistics statistics, List<TaskResult> details) {}
    public String export(Chapter chapter, Attempt a, String displayTitle, Player player) {
        var result = view(chapter,a);
        var visitor = new TextReportVisitor(); result.details().forEach(r -> r.accept(visitor));
        var s = result.statistics();
        return "Путешествие по истории Казахстана\nИгрок: " + player.name() + "\n" + displayTitle + "\nРежим: " + a.mode
                + "\nНачало: " + a.startedAt + "\nЗавершение: " + a.endedAt + "\nВремя, секунд: " + result.seconds()
                + "\nУспешно: " + a.passed + "\nЗаданий: " + s.tasks() + "; верно: " + s.correct() + "; ответов: " + s.tries() + "; ошибок: " + s.errors()
                + "\n\n" + visitor.text() + "Источники:\n" + chapter.sources().stream().map(x -> x.id() + " — " + x.title() + "\n" + x.url()).collect(java.util.stream.Collectors.joining("\n"));
    }
}
