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
        for (var text : new String[]{d.id(), d.title(), d.period(), d.scenePeriod(), d.location(), d.theme(), d.action(), d.narration()})
            if (blank(text)) throw new IllegalArgumentException("Empty chapter field");
        if (d.order() < 1 || d.npcs() == null || d.npcs().size() < 2 || d.sources() == null || d.sources().isEmpty()
                || d.obstacles() == null || !Double.isFinite(d.lon()) || !Double.isFinite(d.lat())
                || Math.abs(d.lon()) > 180 || Math.abs(d.lat()) > 90) throw new IllegalArgumentException("Invalid chapter configuration");
        Set<String> sourceIds = new HashSet<>(), npcIds = new HashSet<>(), taskIds = new HashSet<>();
        for (var s : d.sources()) {
            if (s == null || blank(s.id()) || blank(s.title()) || blank(s.organization()) || blank(s.url()) || !sourceIds.add(s.id()))
                throw new IllegalArgumentException("Invalid source");
            var url = URI.create(s.url());
            if (!"https".equals(url.getScheme()) || url.getHost() == null || url.getUserInfo() != null) throw new IllegalArgumentException("Invalid source URL");
        }
        for (var n : d.npcs()) {
            if (n == null || blank(n.id()) || !npcIds.add(n.id()) || !sourceIds.contains(n.sourceId()) || blank(n.name()) || blank(n.role()) || blank(n.text())
                    || n.x() < 40 || n.x() > 920 || n.y() < 160 || n.y() > 490) throw new IllegalArgumentException("Invalid NPC");
        }
        for (var t : tasks) if (t == null || blank(t.id()) || !taskIds.add(t.id()) || !sourceIds.contains(t.sourceId())) throw new IllegalArgumentException("Invalid task reference");
        if (tasks.stream().filter(t -> "QUIZ".equals(t.kind())).count() < 2 || tasks.stream().noneMatch(t -> "MAP".equals(t.kind())))
            throw new IllegalArgumentException("Required task types missing");
        for (var o : d.obstacles()) {
            if (o == null || blank(o.kind()) || o.x() < 0 || o.y() < 145 || o.w() <= 0 || o.h() <= 0 || o.x() + o.w() > 960 || o.y() + o.h() > 520)
                throw new IllegalArgumentException("Invalid obstacle");
            for (var n : d.npcs()) if (n.x() > o.x()-18 && n.x() < o.x()+o.w()+18 && n.y() > o.y()-18 && n.y() < o.y()+o.h()+18)
                throw new IllegalArgumentException("NPC blocked by obstacle");
        }
        validateReachability(d);
        return new Chapter(d.id(), d.order(), d.title(), d.period(), d.scenePeriod(), d.location(), d.lon(), d.lat(), d.theme(),
                d.action(), d.narration(), List.copyOf(d.npcs()), List.copyOf(d.obstacles()), List.copyOf(tasks), List.copyOf(d.sources()));
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static boolean walkable(ChapterDefinition d,int x,int y) {
        return x >= 25 && x <= 935 && y >= 170 && y <= 510 && d.obstacles().stream().noneMatch(o -> x > o.x()-13 && x < o.x()+o.w()+13 && y > o.y()-10 && y < o.y()+o.h()+10);
    }
    private static void validateReachability(ChapterDefinition d) {
        if (!walkable(d,120,440)) throw new IllegalArgumentException("Safe start is blocked");
        var seen = new HashSet<String>(); var reached = new HashSet<String>(); var queue = new ArrayDeque<int[]>();
        queue.add(new int[]{120,440});
        while (!queue.isEmpty()) {
            var p = queue.remove();
            if (!seen.add(p[0]+":"+p[1]) || !walkable(d,p[0],p[1])) continue;
            for (var n : d.npcs()) if (Math.hypot(n.x()-p[0],n.y()-p[1]) < 60) reached.add(n.id());
            if (reached.size() == d.npcs().size()) return;
            for (var step : new int[][]{{10,0},{-10,0},{0,10},{0,-10}}) queue.add(new int[]{p[0]+step[0],p[1]+step[1]});
        }
        throw new IllegalArgumentException("Required NPC is unreachable");
    }
}
