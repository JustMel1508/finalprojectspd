package edu.historyjourney.model;

public record Player(String id, String name, Avatar avatar) {
    public enum Avatar { HERO, HEROINE }
}
