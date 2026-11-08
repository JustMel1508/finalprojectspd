package edu.historyjourney.model;

import java.time.Instant;
import java.util.*;

/** Persisted aggregate. Mutated only inside JsonGameRepository.transaction. */
public class Attempt {
    public enum State { ACTIVE, COMPLETED, CANCELLED }
    public enum Mode { LEARNING, ASSESSMENT }
    public String id, playerId, chapterId;
    public Mode mode;
    public State state = State.ACTIVE;
    public Set<String> visitedNpcs = new LinkedHashSet<>();
    public Map<String, Answer> answers = new LinkedHashMap<>();
    public Map<String, Receipt> requests = new LinkedHashMap<>();
    public Instant startedAt = Instant.now(), endedAt;
    public boolean passed;
    public record Answer(String taskId, String optionId, boolean correct, int tries, int errors) {}
    public record Receipt(String taskId, String optionId, String feedback) {}

    public void requireActive() {
        if (state != State.ACTIVE) throw new GameException(409, "ATTEMPT_CLOSED", "Попытка уже закрыта.", "Откройте историю или начните новую попытку.");
    }
    public void complete(boolean success) { requireActive(); state = State.COMPLETED; passed = success; endedAt = Instant.now(); }
    public void cancel() { requireActive(); state = State.CANCELLED; endedAt = Instant.now(); }
}
