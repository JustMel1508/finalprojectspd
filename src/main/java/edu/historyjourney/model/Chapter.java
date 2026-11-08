package edu.historyjourney.model;

import edu.historyjourney.task.Task;
import java.util.List;

public record Chapter(String id, int order, String title, String period, String scenePeriod,
        String location, double lon, double lat, String theme, String action, String narration,
        List<Npc> npcs, List<Obstacle> obstacles, List<Task> tasks, List<Source> sources) {
    public record Npc(String id, String name, String role, int x, int y, String text, String sourceId) {}
    public record Obstacle(int x, int y, int w, int h, String kind) {}
    public record Source(String id, String title, String url, String organization) {}
}
