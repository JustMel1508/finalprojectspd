package edu.historyjourney.content;

import edu.historyjourney.model.Chapter;
import edu.historyjourney.task.TaskDefinition;
import java.util.List;

public record ChapterDefinition(String id, int order, String title, String period, String scenePeriod,
        String location, double lon, double lat, String theme, String action, String narration,
        List<Chapter.Npc> npcs, List<Chapter.Obstacle> obstacles, List<TaskDefinition> tasks, List<Chapter.Source> sources) {}
