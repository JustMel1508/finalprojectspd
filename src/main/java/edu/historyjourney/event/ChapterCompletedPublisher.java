package edu.historyjourney.event;
import edu.historyjourney.model.GameState;
import java.util.*;
public class ChapterCompletedPublisher {
    private final List<ChapterCompletedObserver> observers = new ArrayList<>();
    public void subscribe(ChapterCompletedObserver observer) { observers.add(Objects.requireNonNull(observer)); }
    public void publish(ChapterCompleted event, GameState candidate) { observers.forEach(o -> o.onCompleted(event, candidate)); }
}
