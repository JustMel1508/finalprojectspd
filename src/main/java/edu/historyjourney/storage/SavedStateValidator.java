package edu.historyjourney.storage;

import com.fasterxml.jackson.databind.JsonNode;
import edu.historyjourney.content.ContentCatalog;
import edu.historyjourney.model.*;
import edu.historyjourney.strategy.*;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.*;

/** Rejects parseable but inconsistent saves before they become the live state. */
@Component
public class SavedStateValidator {
    private final ContentCatalog catalog;
    public SavedStateValidator(ContentCatalog catalog) { this.catalog = catalog; }

    public void validateShape(JsonNode root) throws IOException {
        fields(root, "formatVersion", "players", "attempts", "progress", "statistics");
        for (String map : List.of("players", "attempts", "progress", "statistics")) require(root.get(map).isObject(), "Invalid " + map);
        for (var p : root.get("players")) fields(p, "id", "name", "avatar");
        for (var p : root.get("progress")) fields(p, "completedChapters", "processedEvents");
        for (var s : root.get("statistics")) fields(s, "processedEvents", "completed", "passed", "failed");
        for (var a : root.get("attempts")) {
            fields(a, "id", "playerId", "chapterId", "mode", "state", "visitedNpcs", "answers", "requests", "startedAt", "endedAt", "passed");
            require(a.get("answers").isObject() && a.get("requests").isObject(), "Invalid answers/requests");
            for (var answer : a.get("answers")) fields(answer, "taskId", "optionId", "correct", "tries", "errors");
            for (var receipt : a.get("requests")) fields(receipt, "taskId", "optionId", "feedback");
        }
    }

    public void validate(GameState s) throws IOException {
        require(s != null && s.formatVersion == 1 && s.players != null && !s.players.isEmpty()
                && s.attempts != null && s.progress != null && s.statistics != null, "Unsupported save format");
        require(s.progress.keySet().equals(s.players.keySet()) && s.statistics.keySet().equals(s.players.keySet()), "Orphan profile data");
        for (var entry : s.players.entrySet()) {
            var p = entry.getValue();
            require(p != null && validId(p.id()) && p.id().equals(entry.getKey()) && p.avatar() != null
                    && p.name() != null && !p.name().isBlank() && p.name().equals(p.name().strip())
                    && p.name().codePointCount(0,p.name().length()) <= 40
                    && p.name().codePoints().noneMatch(Character::isISOControl)
                    && !p.name().codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)), "Invalid player");
        }
        var chapterById = new HashMap<String,Chapter>();
        catalog.all().forEach(c -> chapterById.put(c.id(),c));
        var activePlayers = new HashSet<String>();
        var completed = new HashMap<String,Set<String>>();
        var mastered = new HashMap<String,Set<String>>();
        var successes = new HashMap<String,Integer>();
        s.players.keySet().forEach(p -> { completed.put(p,new HashSet<>()); mastered.put(p,new HashSet<>()); successes.put(p,0); });
        for (var entry : s.attempts.entrySet()) {
            var a = entry.getValue();
            require(a != null && uuid(a.id) && a.id.equals(entry.getKey()) && s.players.containsKey(a.playerId)
                    && chapterById.containsKey(a.chapterId) && a.mode != null && a.state != null && a.startedAt != null
                    && a.visitedNpcs != null && a.answers != null && a.requests != null, "Invalid attempt");
            require(a.state == Attempt.State.ACTIVE ? a.endedAt == null && !a.passed && activePlayers.add(a.playerId)
                    : a.endedAt != null && !a.endedAt.isBefore(a.startedAt), "Invalid attempt transition/time");
            require(a.state != Attempt.State.CANCELLED || !a.passed, "Cancelled attempt cannot pass");
            var c = chapterById.get(a.chapterId);
            var npcIds = new HashSet<String>(); c.npcs().forEach(n -> npcIds.add(n.id()));
            require(npcIds.containsAll(a.visitedNpcs), "Unknown visited NPC");
            require(a.requests.isEmpty() || a.visitedNpcs.containsAll(npcIds), "Answers before required conversations");
            AnswerCheckingStrategy strategy = a.mode == Attempt.Mode.LEARNING ? new LearningStrategy() : new AssessmentStrategy();
            var replayed = new LinkedHashMap<String,Attempt.Answer>();
            var canonicalRequests = new HashSet<String>();
            for (var request : a.requests.entrySet()) {
                var receipt = request.getValue();
                require(uuid(request.getKey()) && receipt != null, "Invalid request receipt");
                require(canonicalRequests.add(UUID.fromString(request.getKey()).toString()), "Duplicate UUID receipt");
                var task = c.tasks().stream().filter(t -> !strategy.canAdvance(replayed.get(t.id()))).findFirst().orElse(null);
                require(task != null && task.id().equals(receipt.taskId()) && task.options().stream().anyMatch(o -> o.id().equals(receipt.optionId())), "Invalid receipt task/option/order");
                var checked = strategy.check(task,receipt.optionId(),replayed.get(task.id()));
                require(strategy.feedback(task,checked).equals(receipt.feedback()), "Inconsistent receipt feedback");
                replayed.put(task.id(),checked);
            }
            require(replayed.equals(a.answers), "Answers disagree with accepted requests");
            if (a.state == Attempt.State.COMPLETED) {
                require(a.visitedNpcs.containsAll(npcIds) && c.tasks().stream().allMatch(t -> strategy.canAdvance(a.answers.get(t.id()))), "Incomplete completed attempt");
                require(a.passed == a.answers.values().stream().allMatch(Attempt.Answer::correct), "Inconsistent passed result");
                completed.get(a.playerId).add(a.id);
                if (a.passed) { mastered.get(a.playerId).add(a.chapterId); successes.merge(a.playerId,1,Integer::sum); }
            }
        }
        for (var id : s.players.keySet()) {
            var progress = s.progress.get(id); var stats = s.statistics.get(id);
            require(progress != null && progress.completedChapters != null && progress.processedEvents != null
                    && progress.completedChapters.equals(mastered.get(id)) && progress.processedEvents.equals(completed.get(id)), "Inconsistent progress");
            require(stats != null && stats.processedEvents != null && stats.processedEvents.equals(completed.get(id))
                    && stats.completed == completed.get(id).size() && stats.passed == successes.get(id)
                    && stats.failed == stats.completed-stats.passed, "Inconsistent completion statistics");
            for (var c : catalog.all()) if (mastered.get(id).contains(c.id()) && c.order() > 1)
                require(mastered.get(id).contains(catalog.all().get(c.order()-2).id()), "Nonsequential mastered chapters");
        }
        for (var a : s.attempts.values()) {
            var c = chapterById.get(a.chapterId);
            if (c.order() > 1) require(mastered.get(a.playerId).contains(catalog.all().get(c.order()-2).id()), "Attempt for a locked chapter");
        }
    }

    private static boolean validId(String id) { return id != null && !id.isBlank(); }
    private static boolean uuid(String id) {
        if (id == null) return false;
        try { return UUID.fromString(id).toString().equalsIgnoreCase(id); } catch (IllegalArgumentException e) { return false; }
    }
    private static void fields(JsonNode node,String... names) throws IOException {
        require(node != null && node.isObject(), "Expected JSON object");
        for (var name : names) require(node.has(name), "Missing field: " + name);
    }
    private static void require(boolean condition,String message) throws IOException { if (!condition) throw new IOException(message); }
}
