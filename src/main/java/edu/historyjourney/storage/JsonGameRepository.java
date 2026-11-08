package edu.historyjourney.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.historyjourney.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final SavedStateValidator validator;
    private GameState state;
    private String problem;
    @Autowired
    public JsonGameRepository(ObjectMapper mapper, @Value("${game.data-dir:data}") String directory, SavedStateValidator validator) {
        this.mapper = mapper; this.directory = Path.of(directory).toAbsolutePath(); this.validator = validator;
        try {
            Files.createDirectories(this.directory);
            var file = this.directory.resolve("save.json");
            if (Files.exists(file)) {
                var tree = mapper.copy().enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readTree(file.toFile());
                validator.validateShape(tree);
                state = mapper.treeToValue(tree, GameState.class); validator.validate(state);
            }
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
    /** Convenience constructor for isolated tests without a Spring container. */
    public JsonGameRepository(ObjectMapper mapper, String directory) {
        this(mapper,directory,defaultValidator(mapper));
    }
    private static SavedStateValidator defaultValidator(ObjectMapper mapper) {
        try { return new SavedStateValidator(new edu.historyjourney.content.ContentCatalog(mapper)); }
        catch (IOException e) { throw new IllegalArgumentException("Cannot load game content",e); }
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
        try {
            validator.validate(candidate);
            if (!java.util.Arrays.equals(mapper.writeValueAsBytes(state),mapper.writeValueAsBytes(candidate))) persist(candidate);
        }
        catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger(JsonGameRepository.class).warn("Save transaction rolled back: {}", e.toString());
            throw new GameException(503, "SAVE_FAILED", "Не удалось записать сохранение. Предыдущее состояние сохранено.", "Проверьте свободное место и доступ к папке data; повторите действие.");
        }
        state = candidate;
        return result;
    }
    protected void persist(GameState value) throws IOException {
        Path temp = Files.createTempFile(directory, "save-", ".tmp");
        try {
            Files.write(temp, mapper.writeValueAsBytes(value), TRUNCATE_EXISTING);
            try (var channel = FileChannel.open(temp, WRITE)) { channel.force(true); }
            // Windows indexers/sync tools can briefly hold the destination open.
            // Every retry still uses an atomic replacement; persistent failure rolls back.
            for (int retry=0; ; retry++) {
                try {
                    Files.move(temp, directory.resolve("save.json"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    break;
                } catch (AccessDeniedException e) {
                    if (retry >= 5) throw e;
                    try { Thread.sleep(30L*(retry+1)); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException("Save interrupted",interrupted); }
                }
            }
        } finally { Files.deleteIfExists(temp); }
    }
}
