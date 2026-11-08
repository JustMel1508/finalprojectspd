package edu.historyjourney.model;

import java.util.*;

public class GameState {
    public int formatVersion = 1;
    public Map<String, Player> players = new LinkedHashMap<>();
    public Map<String, Attempt> attempts = new LinkedHashMap<>();
    public Map<String, PlayerProgress> progress = new LinkedHashMap<>();
    public Map<String, CompletionStats> statistics = new LinkedHashMap<>();
    public static class CompletionStats {
        public Set<String> processedEvents = new LinkedHashSet<>();
        public int completed, passed, failed;
    }
}
