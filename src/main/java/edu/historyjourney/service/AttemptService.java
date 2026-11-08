package edu.historyjourney.service;

import edu.historyjourney.model.*;
import edu.historyjourney.strategy.*;
import edu.historyjourney.task.Task;
import java.util.*;

public class AttemptService {
    private final Map<Attempt.Mode,AnswerCheckingStrategy> strategies = Map.of(Attempt.Mode.LEARNING, new LearningStrategy(), Attempt.Mode.ASSESSMENT,new AssessmentStrategy());
    public AnswerCheckingStrategy strategy(Attempt a) { return strategies.get(a.mode); }
    public Task currentTask(Chapter chapter, Attempt a) {
        return chapter.tasks().stream().filter(t -> !strategy(a).canAdvance(a.answers.get(t.id()))).findFirst().orElse(null);
    }
    public void requireTalks(Chapter c, Attempt a) {
        if (!c.npcs().stream().allMatch(n -> a.visitedNpcs.contains(n.id()))) throw GameException.conflict("Сначала поговорите со всеми собеседниками.");
    }
    public boolean available(Chapter c, PlayerProgress progress, List<Chapter> catalog) {
        return c.order() == 1 || progress.completedChapters.contains(catalog.get(c.order()-2).id());
    }
    public String title(Chapter c, PlayerProgress progress) {
        return progress.completedChapters.contains(c.id()) ? c.title() + " · " + c.period() : "Уровень " + c.order();
    }
}
