package edu.historyjourney.event;
public record ChapterCompleted(String attemptId, String playerId, String chapterId, boolean passed) {}
