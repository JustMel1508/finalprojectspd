package edu.historyjourney.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.historyjourney.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.function.Function;
import static java.nio.file.StandardOpenOption.*;

/** Serialized copy-on-write transactions: disk commit precedes publication in memory. */
@Component
public class JsonGameRepository implements GameRepository {
    private final ObjectMapper mapper;
    private final Path directory;
    private GameState state;
    private String problem;
    public JsonGameRepository(ObjectMapper mapper, @Value("${game.data-dir:data}") String directory) {
        this.mapper = mapper; this.directory = Path.of(directory).toAbsolutePath();
        try {
            Files.createDirectories(this.directory);
            var file = this.directory.resolve("save.json");
            if (Files.exists(file)) { state = mapper.readValue(file.toFile(), GameState.class); validate(state); }
            else {
                state = new GameState();
                state.players.put("demo", new Player("demo", "Путешественник", Player.Avatar.HEROINE));
                state.progress.put("demo", new PlayerProgress());
                state.statistics.put("demo", new GameState.CompletionStats());
                persist(state);
            }
        } catch (Exception e) {
            problem = "Сохранение не прочитано: " + this.directory + ". Сохраните копию save.json, исправьте файл или переименуйте его и перезапустите приложение. Исходный файл не перезаписан.";
        }
    }
    private void validate(GameState s) throws IOException {
        if (s == null || s.formatVersion != 1 || s.players == null || s.attempts == null || s.progress == null || s.statistics == null)
            throw new IOException("Unsupported save format");
        for (var p : s.players.values()) if (p.id() == null || p.name() == null || p.avatar() == null || !s.progress.containsKey(p.id()) || !s.statistics.containsKey(p.id()))
            throw new IOException("Invalid player");
        for (var a : s.attempts.values()) if (!s.players.containsKey(a.playerId) || a.state == null || a.mode == null || a.chapterId == null
                || a.answers == null || a.visitedNpcs == null || a.requests == null || a.startedAt == null)
            throw new IOException("Invalid attempt");
    }
    @Override public synchronized String problem() { return problem; }
    private void available() {
        if (problem != null) throw new GameException(503, "SAVE_UNAVAILABLE", problem, "Исправьте сохранение и перезапустите сервер.");
    }
    private GameState copy(GameState s) {
        try { return mapper.readValue(mapper.writeValueAsBytes(s), GameState.class); }
        catch (IOException e) { throw new IllegalStateException("State cannot be copied", e); }
    }
    @Override public synchronized <T> T read(Function<GameState,T> query) { available(); return query.apply(copy(state)); }
    @Override public synchronized <T> T transaction(Function<GameState,T> operation) {
        available();
        var candidate = copy(state);
        T result = operation.apply(candidate);
        try { validate(candidate); persist(candidate); }
        catch (IOException e) { throw new GameException(503, "SAVE_FAILED", "Не удалось записать сохранение. Предыдущее состояние сохранено.", "Проверьте свободное место и доступ к папке data; повторите действие."); }
        state = candidate;
        return result;
    }
    protected void persist(GameState value) throws IOException {
        Path temp = Files.createTempFile(directory, "save-", ".tmp");
        try {
            Files.write(temp, mapper.writeValueAsBytes(value), TRUNCATE_EXISTING);
            try (var channel = FileChannel.open(temp, WRITE)) { channel.force(true); }
            Files.move(temp, directory.resolve("save.json"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
