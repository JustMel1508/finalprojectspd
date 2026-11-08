package edu.historyjourney.storage;
import edu.historyjourney.model.GameState;
import java.util.function.Function;
public interface GameRepository {
    <T> T read(Function<GameState,T> query);
    <T> T transaction(Function<GameState,T> operation);
    String problem();
}
