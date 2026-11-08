package edu.historyjourney.service;

import edu.historyjourney.content.ContentCatalog;
import edu.historyjourney.event.*;
import edu.historyjourney.model.*;
import edu.historyjourney.result.ResultService;
import edu.historyjourney.storage.GameRepository;
import edu.historyjourney.task.Task;
import org.springframework.stereotype.Service;
import java.util.*;

/** Facade coordinates use cases; rules and processing live in focused services. */
@Service
public class GameFacade {
    private final GameRepository repository;
    private final ContentCatalog catalog;
    private final ProfileService profiles = new ProfileService();
    private final AttemptService attempts = new AttemptService();
    private final ResultService results = new ResultService();
    private final ChapterCompletedPublisher publisher = new ChapterCompletedPublisher();
    public GameFacade(GameRepository repository, ContentCatalog catalog) {
        this.repository=repository; this.catalog=catalog;
        publisher.subscribe(new ProgressObserver()); publisher.subscribe(new CompletionStatisticsObserver());
    }
    public record MapEntry(String id, int order, String displayTitle, String status, double lon, double lat) {}
    public record TaskView(String id, String kind, String prompt, List<Task.Option> options) {}
    public record SceneView(String location, String period, String theme, String action, String narration,
            List<Chapter.Npc> npcs, List<Chapter.Obstacle> obstacles, List<Chapter.Source> sources) {}
    public record AttemptView(String id, String chapterId, int order, String displayTitle, Attempt.Mode mode,
            Attempt.State state, Set<String> visitedNpcs, int acceptedTasks, int totalTasks,
            SceneView scene, TaskView currentTask, boolean readyToFinish, String feedback, ResultService.ResultView result) {}
    public record HistoryEntry(String id, String displayTitle, Attempt.Mode mode, Attempt.State state, Boolean passed, String startedAt) {}
    public record Summary(int completedChapters, int totalChapters, int completedAttempts, int passedAttempts, int failedAttempts,
            long cancelledAttempts, List<MapEntry> chapters, boolean finished) {}

    public List<Player> players() { return repository.read(s -> List.copyOf(s.players.values())); }
    public Player savePlayer(String id, String name, Player.Avatar avatar) { return repository.transaction(s -> profiles.save(s,id,name,avatar)); }
    private Player player(GameState s, String id) { var p=s.players.get(id); if(p==null) throw GameException.missing(); return p; }
    private Attempt owned(GameState s, String playerId, String id) {
        player(s,playerId); var a=s.attempts.get(id);
        if(a==null || !a.playerId.equals(playerId)) throw GameException.missing(); return a;
    }
    private List<MapEntry> map(GameState s, String playerId) {
        player(s,playerId); var progress=s.progress.get(playerId);
        return catalog.all().stream().map(c -> new MapEntry(c.id(),c.order(),attempts.title(c,progress),
                progress.completedChapters.contains(c.id()) ? "COMPLETED" : attempts.available(c,progress,catalog.all()) ? "AVAILABLE" : "LOCKED",c.lon(),c.lat())).toList();
    }
    public List<MapEntry> map(String playerId) { return repository.read(s -> map(s,playerId)); }
    public Summary summary(String playerId) { return repository.read(s -> summary(s,playerId)); }
    private Summary summary(GameState s, String playerId) {
        var entries=map(s,playerId); var stats=s.statistics.get(playerId);
        int count=(int)entries.stream().filter(c -> c.status().equals("COMPLETED")).count();
        long cancelled=s.attempts.values().stream().filter(a -> a.playerId.equals(playerId) && a.state==Attempt.State.CANCELLED).count();
        return new Summary(count,entries.size(),stats.completed,stats.passed,stats.failed,cancelled,entries,count==entries.size());
    }
    public List<HistoryEntry> history(String playerId) {
        return repository.read(s -> { player(s,playerId); return s.attempts.values().stream().filter(a -> a.playerId.equals(playerId))
                .sorted(Comparator.comparing((Attempt a) -> a.startedAt).reversed())
                .map(a -> new HistoryEntry(a.id,attempts.title(catalog.get(a.chapterId),s.progress.get(playerId)),a.mode,a.state,a.state==Attempt.State.COMPLETED ? a.passed : null,a.startedAt.toString())).toList(); });
    }
    public AttemptView active(String playerId) {
        return repository.read(s -> { player(s,playerId); return s.attempts.values().stream().filter(a -> a.playerId.equals(playerId)&&a.state==Attempt.State.ACTIVE)
                .findFirst().map(a -> view(s,a,"")).orElse(null); });
    }
    public AttemptView start(String playerId, String chapterId, Attempt.Mode mode) {
        return repository.transaction(s -> {
            player(s,playerId); var c=catalog.get(chapterId);
            if(mode==null) throw GameException.invalid("Выберите режим.");
            if(!attempts.available(c,s.progress.get(playerId),catalog.all())) throw GameException.conflict("Глава пока закрыта. Завершите предыдущую.");
            if(s.attempts.values().stream().anyMatch(a -> a.playerId.equals(playerId)&&a.state==Attempt.State.ACTIVE)) throw GameException.conflict("Продолжите или отмените текущую попытку.");
            var a=new Attempt(); a.id=UUID.randomUUID().toString(); a.playerId=playerId; a.chapterId=chapterId; a.mode=mode;
            s.attempts.put(a.id,a); return view(s,a,"Попытка сохранена. Найдите двух собеседников.");
        });
    }
    public AttemptView get(String playerId,String id) { return repository.read(s -> view(s,owned(s,playerId,id),"")); }
    public AttemptView talk(String playerId,String id,String npcId) {
        return repository.transaction(s -> { var a=owned(s,playerId,id); a.requireActive(); var c=catalog.get(a.chapterId);
            if(c.npcs().stream().noneMatch(n -> n.id().equals(npcId))) throw GameException.invalid("Неизвестный собеседник.");
            a.visitedNpcs.add(npcId); return view(s,a,"Разговор сохранён."); });
    }
    public AttemptView answer(String playerId,String id,String requestId,String taskId,String optionId) {
        return repository.transaction(s -> {
            var a=owned(s,playerId,id); var c=catalog.get(a.chapterId);
            String canonicalRequestId;
            try {
                canonicalRequestId = UUID.fromString(requestId).toString();
                if (!canonicalRequestId.equalsIgnoreCase(requestId)) throw new IllegalArgumentException();
            }
            catch(Exception e) { throw GameException.invalid("requestId должен быть UUID."); }
            var receipt=a.requests.get(canonicalRequestId);
            if(receipt!=null) {
                if(!receipt.taskId().equals(taskId)||!receipt.optionId().equals(optionId)) throw GameException.conflict("Этот requestId уже использован для другого ответа.");
                return view(s,a,receipt.feedback());
            }
            a.requireActive(); attempts.requireTalks(c,a); var task=attempts.currentTask(c,a);
            if(task==null||!task.id().equals(taskId)) throw GameException.conflict("Отвечайте на текущее задание по порядку.");
            if(task.options().stream().noneMatch(o -> o.id().equals(optionId))) throw GameException.invalid("Выберите допустимый вариант ответа.");
            var strategy=attempts.strategy(a); var answer=strategy.check(task,optionId,a.answers.get(taskId));
            a.answers.put(taskId,answer); var feedback=strategy.feedback(task,answer);
            a.requests.put(canonicalRequestId,new Attempt.Receipt(taskId,optionId,feedback)); return view(s,a,feedback);
        });
    }
    public AttemptView finish(String playerId,String id) {
        return repository.transaction(s -> {
            var a=owned(s,playerId,id); if(a.state==Attempt.State.COMPLETED) return view(s,a,"Результат и прогресс сохранены.");
            a.requireActive(); var c=catalog.get(a.chapterId); attempts.requireTalks(c,a);
            if(attempts.currentTask(c,a)!=null) throw GameException.conflict("Сначала выполните все задания.");
            a.complete(a.answers.values().stream().allMatch(Attempt.Answer::correct));
            publisher.publish(new ChapterCompleted(a.id,a.playerId,a.chapterId,a.passed),s);
            return view(s,a,"Результат и прогресс сохранены.");
        });
    }
    public AttemptView cancel(String playerId,String id) {
        return repository.transaction(s -> { var a=owned(s,playerId,id); if(a.state!=Attempt.State.CANCELLED) a.cancel(); return view(s,a,"Попытка отменена. Освоенные главы сохранены."); });
    }
    public String export(String playerId,String id) {
        return repository.read(s -> { var a=owned(s,playerId,id); if(a.state!=Attempt.State.COMPLETED) throw GameException.conflict("Экспорт доступен после завершения.");
            var c=catalog.get(a.chapterId); return results.export(c,a,attempts.title(c,s.progress.get(playerId)),player(s,playerId)); });
    }
    public String exportSummary(String playerId) {
        return repository.read(s -> { var summary=summary(s,playerId);
            var text=new StringBuilder("Путешествие по истории Казахстана\nИгрок: "+player(s,playerId).name()+"\nПройдено: "+summary.completedChapters()+" / "+summary.totalChapters()
                    +"\nЗавершённых попыток: "+summary.completedAttempts()+"; успешных: "+summary.passedAttempts()+"; неуспешных: "+summary.failedAttempts()+"; отменённых: "+summary.cancelledAttempts()+"\n\n");
            summary.chapters().forEach(c -> text.append(c.displayTitle()).append(" — ").append(c.status()).append("\n"));
            s.attempts.values().stream().filter(a -> a.playerId.equals(playerId)&&a.state==Attempt.State.COMPLETED).forEach(a -> text.append("\n================\n").append(results.export(catalog.get(a.chapterId),a,attempts.title(catalog.get(a.chapterId),s.progress.get(playerId)),s.players.get(playerId))));
            return text.toString(); });
    }
    private AttemptView view(GameState s,Attempt a,String feedback) {
        var c=catalog.get(a.chapterId); boolean active=a.state==Attempt.State.ACTIVE;
        boolean talked=c.npcs().stream().allMatch(n -> a.visitedNpcs.contains(n.id()));
        Task current=active&&talked?attempts.currentTask(c,a):null;
        int accepted=(int)c.tasks().stream().filter(t -> attempts.strategy(a).canAdvance(a.answers.get(t.id()))).count();
        var scene=active?new SceneView(c.location(),c.scenePeriod(),c.theme(),c.action(),talked?c.narration():"",
                c.npcs(),c.obstacles(),c.sources()):null;
        return new AttemptView(a.id,a.chapterId,c.order(),attempts.title(c,s.progress.get(a.playerId)),a.mode,a.state,
                Set.copyOf(a.visitedNpcs),accepted,c.tasks().size(),scene,
                current==null?null:new TaskView(current.id(),current.kind(),current.prompt(),current.options()),active&&talked&&current==null,feedback,
                a.state==Attempt.State.COMPLETED?results.view(c,a):null);
    }
}
