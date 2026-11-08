package edu.historyjourney.content;

import edu.historyjourney.model.Chapter;
import edu.historyjourney.task.Task;
import java.net.URI;
import java.util.*;

/** Builder separates validated assembly from the content loader. */
public class ChapterBuilder {
    private ChapterDefinition definition;
    private List<Task> tasks;
    public ChapterBuilder definition(ChapterDefinition d) { definition = d; return this; }
    public ChapterBuilder tasks(List<Task> value) { tasks = value; return this; }
    public Chapter build() {
        var d = definition;
        if (d == null || tasks == null || tasks.size() < 3) throw new IllegalArgumentException("Incomplete chapter");
        for (var text : List.of(d.id(), d.title(), d.period(), d.scenePeriod(), d.location(), d.theme(), d.action(), d.narration()))
            if (text.isBlank()) throw new IllegalArgumentException("Empty chapter field");
        if (d.order() < 1 || d.npcs() == null || d.npcs().size() < 2 || d.sources() == null || d.sources().isEmpty()
                || d.obstacles() == null || !Double.isFinite(d.lon()) || !Double.isFinite(d.lat())
                || Math.abs(d.lon()) > 180 || Math.abs(d.lat()) > 90) throw new IllegalArgumentException("Invalid chapter configuration");
        Set<String> sourceIds = new HashSet<>(), npcIds = new HashSet<>(), taskIds = new HashSet<>();
        for (var s : d.sources()) {
            if (!sourceIds.add(s.id()) || !"https".equals(URI.create(s.url()).getScheme())) throw new IllegalArgumentException("Invalid source");
        }
        for (var n : d.npcs()) {
            if (!npcIds.add(n.id()) || !sourceIds.contains(n.sourceId()) || n.name().isBlank() || n.text().isBlank()
                    || n.x() < 40 || n.x() > 920 || n.y() < 160 || n.y() > 490) throw new IllegalArgumentException("Invalid NPC");
        }
        for (var t : tasks) if (!taskIds.add(t.id()) || !sourceIds.contains(t.sourceId())) throw new IllegalArgumentException("Invalid task reference");
        if (tasks.stream().filter(t -> "QUIZ".equals(t.kind())).count() < 2 || tasks.stream().noneMatch(t -> "MAP".equals(t.kind())))
            throw new IllegalArgumentException("Required task types missing");
        for (var o : d.obstacles()) {
            if (o.x() < 0 || o.y() < 145 || o.w() <= 0 || o.h() <= 0 || o.x() + o.w() > 960 || o.y() + o.h() > 520)
                throw new IllegalArgumentException("Invalid obstacle");
            for (var n : d.npcs()) if (n.x() > o.x()-18 && n.x() < o.x()+o.w()+18 && n.y() > o.y()-18 && n.y() < o.y()+o.h()+18)
                throw new IllegalArgumentException("NPC blocked by obstacle");
        }
        return new Chapter(d.id(), d.order(), d.title(), d.period(), d.scenePeriod(), d.location(), d.lon(), d.lat(), d.theme(),
                d.action(), d.narration(), List.copyOf(d.npcs()), List.copyOf(d.obstacles()), List.copyOf(tasks), List.copyOf(d.sources()));
    }
}
