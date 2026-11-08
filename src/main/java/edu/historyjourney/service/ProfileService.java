package edu.historyjourney.service;

import edu.historyjourney.model.*;
import java.util.UUID;

public class ProfileService {
    public Player save(GameState state, String id, String name, Player.Avatar avatar) {
        if (name == null || name.isBlank() || name.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)) || name.strip().codePointCount(0,name.strip().length()) > 40
                || name.codePoints().anyMatch(Character::isISOControl) || avatar == null)
            throw GameException.invalid("Имя должно содержать от 1 до 40 символов; выберите аватар.");
        if (id != null && !state.players.containsKey(id)) throw GameException.missing();
        boolean created = id == null;
        var p = new Player(created ? UUID.randomUUID().toString() : id, name.strip(), avatar);
        state.players.put(p.id(),p);
        if (created) { state.progress.put(p.id(), new PlayerProgress()); state.statistics.put(p.id(),new GameState.CompletionStats()); }
        return p;
    }
}
