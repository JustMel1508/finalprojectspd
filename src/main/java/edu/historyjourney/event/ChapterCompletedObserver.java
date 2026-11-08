package edu.historyjourney.event;
import edu.historyjourney.model.GameState;
public interface ChapterCompletedObserver { void onCompleted(ChapterCompleted event, GameState candidate); }
