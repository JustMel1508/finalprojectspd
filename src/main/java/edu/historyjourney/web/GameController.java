package edu.historyjourney.web;

import edu.historyjourney.model.*;
import edu.historyjourney.service.GameFacade;
import edu.historyjourney.storage.GameRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GameController {
    private final GameFacade game;
    private final GameRepository repository;
    public GameController(GameFacade game,GameRepository repository) { this.game=game; this.repository=repository; }
    public record ProfileInput(@NotBlank @Size(max=80) String name,@NotNull Player.Avatar avatar) {}
    public record StartInput(@NotBlank String chapterId,@NotNull Attempt.Mode mode) {}
    public record TalkInput(@NotBlank String npcId) {}
    public record AnswerInput(@NotBlank @Size(max=50) String requestId,@NotBlank @Size(max=80) String taskId,@NotBlank @Size(max=80) String optionId) {}
    @GetMapping("/health") public Object health() { return Map.of("ready",repository.problem()==null,"message",repository.problem()==null?"Готово":repository.problem()); }
    @GetMapping("/players") public Object players() { return game.players(); }
    @PostMapping("/players") @ResponseStatus(HttpStatus.CREATED) public Object create(@Valid @RequestBody ProfileInput input) { return game.savePlayer(null,input.name(),input.avatar()); }
    @PutMapping("/players/{p}") public Object edit(@PathVariable String p,@Valid @RequestBody ProfileInput input) { return game.savePlayer(p,input.name(),input.avatar()); }
    @GetMapping("/players/{p}/map") public Object map(@PathVariable String p) { return game.map(p); }
    @GetMapping("/players/{p}/summary") public Object summary(@PathVariable String p) { return game.summary(p); }
    @GetMapping("/players/{p}/history") public Object history(@PathVariable String p) { return game.history(p); }
    @GetMapping("/players/{p}/active") public ResponseEntity<?> active(@PathVariable String p) { return ResponseEntity.ok(game.active(p)); }
    @PostMapping("/players/{p}/attempts") @ResponseStatus(HttpStatus.CREATED) public Object start(@PathVariable String p,@Valid @RequestBody StartInput input) { return game.start(p,input.chapterId(),input.mode()); }
    @GetMapping("/players/{p}/attempts/{a}") public Object get(@PathVariable String p,@PathVariable String a) { return game.get(p,a); }
    @PostMapping("/players/{p}/attempts/{a}/talk") public Object talk(@PathVariable String p,@PathVariable String a,@Valid @RequestBody TalkInput input) { return game.talk(p,a,input.npcId()); }
    @PostMapping("/players/{p}/attempts/{a}/answers") public Object answer(@PathVariable String p,@PathVariable String a,@Valid @RequestBody AnswerInput input) { return game.answer(p,a,input.requestId(),input.taskId(),input.optionId()); }
    @PostMapping("/players/{p}/attempts/{a}/finish") public Object finish(@PathVariable String p,@PathVariable String a) { return game.finish(p,a); }
    @PostMapping("/players/{p}/attempts/{a}/cancel") public Object cancel(@PathVariable String p,@PathVariable String a) { return game.cancel(p,a); }
    private ResponseEntity<byte[]> text(String body,String filename) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+filename+"\"")
                .contentType(new MediaType("text","plain",StandardCharsets.UTF_8)).body(body.getBytes(StandardCharsets.UTF_8));
    }
    @GetMapping("/players/{p}/attempts/{a}/export") public Object export(@PathVariable String p,@PathVariable String a) { return text(game.export(p,a),"chapter-result.txt"); }
    @GetMapping("/players/{p}/export") public Object exportSummary(@PathVariable String p) { return text(game.exportSummary(p),"journey-result.txt"); }
}
