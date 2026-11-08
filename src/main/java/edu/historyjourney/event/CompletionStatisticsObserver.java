package edu.historyjourney.event;
import edu.historyjourney.model.GameState;
public class CompletionStatisticsObserver implements ChapterCompletedObserver {
    @Override public void onCompleted(ChapterCompleted event, GameState candidate) {
        var stats = candidate.statistics.get(event.playerId());
        if (!stats.processedEvents.add(event.attemptId())) return;
        stats.completed++;
        if (event.passed()) stats.passed++; else stats.failed++;
    }
}
