package edu.historyjourney.event;
import edu.historyjourney.model.GameState;
public class ProgressObserver implements ChapterCompletedObserver {
    @Override public void onCompleted(ChapterCompleted event, GameState candidate) {
        var progress = candidate.progress.get(event.playerId());
        if (progress.processedEvents.add(event.attemptId()) && event.passed()) progress.completedChapters.add(event.chapterId());
    }
}
