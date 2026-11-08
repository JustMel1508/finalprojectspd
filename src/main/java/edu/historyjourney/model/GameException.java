package edu.historyjourney.model;

public class GameException extends RuntimeException {
    private final int status;
    private final String code, action;
    public GameException(int status, String code, String message, String action) {
        super(message); this.status = status; this.code = code; this.action = action;
    }
    public int status() { return status; }
    public String code() { return code; }
    public String action() { return action; }
    public static GameException invalid(String message) { return new GameException(400, "INVALID_INPUT", message, "Проверьте введённые данные."); }
    public static GameException conflict(String message) { return new GameException(409, "INVALID_STAGE", message, "Обновите состояние и продолжите с доступного этапа."); }
    public static GameException missing() { return new GameException(404, "NOT_FOUND", "Объект не найден.", "Выберите существующий профиль или попытку."); }
}
