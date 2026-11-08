package edu.historyjourney;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.historyjourney.content.*;
import edu.historyjourney.model.*;
import edu.historyjourney.service.GameFacade;
import edu.historyjourney.storage.*;
import edu.historyjourney.task.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class SavedStateValidationTest {
    @TempDir Path directory;
    ObjectMapper mapper; ContentCatalog catalog; JsonGameRepository repository; GameFacade game;
    String completedId,activeId;
    @BeforeEach void setup() throws Exception {
        mapper = new ObjectMapper().findAndRegisterModules(); catalog = new ContentCatalog(mapper);
        repository = new JsonGameRepository(mapper,directory.toString(),new SavedStateValidator(catalog)); game = new GameFacade(repository,catalog);
        var first = game.start("demo","c1",Attempt.Mode.LEARNING); completedId=first.id();
        answerChapter(first);
        var second = game.start("demo","c2",Attempt.Mode.ASSESSMENT); activeId=second.id();
        for (var npc : catalog.get("c2").npcs()) game.talk("demo",activeId,npc.id());
        var task = catalog.get("c2").tasks().get(0);
        game.answer("demo",activeId,UUID.randomUUID().toString(),task.id(),task.correctOptionId());
    }
    void answerChapter(GameFacade.AttemptView view) {
        var chapter = catalog.get(view.chapterId());
        for (var npc : chapter.npcs()) game.talk("demo",view.id(),npc.id());
        for (var task : chapter.tasks()) game.answer("demo",view.id(),UUID.randomUUID().toString(),task.id(),task.correctOptionId());
        game.finish("demo",view.id());
    }
    static Stream<Arguments> inconsistentSaves() {
        return Stream.of(
            Arguments.of("unknown chapter",(Consumer<ObjectNode>)r -> active(r).put("chapterId","unknown")),
            Arguments.of("unknown NPC",(Consumer<ObjectNode>)r -> active(r).withArray("visitedNpcs").add("unknown")),
            Arguments.of("unknown option",(Consumer<ObjectNode>)r -> ((ObjectNode)active(r).path("answers").path("c2-q1")).put("optionId","unknown")),
            Arguments.of("changed correctness",(Consumer<ObjectNode>)r -> ((ObjectNode)active(r).path("answers").path("c2-q1")).put("correct",false)),
            Arguments.of("invalid counter",(Consumer<ObjectNode>)r -> ((ObjectNode)active(r).path("answers").path("c2-q1")).put("tries",0)),
            Arguments.of("fabricated progress",(Consumer<ObjectNode>)r -> ((ObjectNode)r.path("progress").path("demo")).withArray("completedChapters").add("c2")),
            Arguments.of("missing completion events",(Consumer<ObjectNode>)r -> ((ObjectNode)r.path("progress").path("demo")).putArray("processedEvents")),
            Arguments.of("inconsistent statistics",(Consumer<ObjectNode>)r -> ((ObjectNode)r.path("statistics").path("demo")).put("completed",50)),
            Arguments.of("answers before NPCs",(Consumer<ObjectNode>)r -> active(r).putArray("visitedNpcs")),
            Arguments.of("changed passed",(Consumer<ObjectNode>)r -> completed(r).put("passed",false)),
            Arguments.of("ended active attempt",(Consumer<ObjectNode>)r -> active(r).put("endedAt","2099-01-01T00:00:00Z")),
            Arguments.of("missing required field",(Consumer<ObjectNode>)r -> active(r).remove("state")),
            Arguments.of("empty profile name",(Consumer<ObjectNode>)r -> ((ObjectNode)r.path("players").path("demo")).put("name"," ")),
            Arguments.of("null progress",(Consumer<ObjectNode>)r -> ((ObjectNode)r.path("progress")).putNull("demo")),
            Arguments.of("empty root",(Consumer<ObjectNode>)ObjectNode::removeAll)
        );
    }
    static ObjectNode active(ObjectNode root) { return find(root,"ACTIVE"); }
    static ObjectNode completed(ObjectNode root) { return find(root,"COMPLETED"); }
    static ObjectNode find(ObjectNode root,String state) {
        for (var value : root.path("attempts")) if (value.path("state").asText().equals(state)) return (ObjectNode)value;
        throw new IllegalStateException("Missing test attempt");
    }
    @ParameterizedTest(name="Reject and preserve {0}") @MethodSource("inconsistentSaves")
    void parseableButInconsistentSaveIsRejectedAndPreserved(String name,Consumer<ObjectNode> change) throws Exception {
        var file = directory.resolve("save.json"); var root=(ObjectNode)mapper.readTree(file.toFile()); change.accept(root);
        var bytes=mapper.writeValueAsBytes(root); Files.write(file,bytes);
        var reload=new JsonGameRepository(mapper,directory.toString(),new SavedStateValidator(catalog));
        assertNotNull(reload.problem(),name); assertArrayEquals(bytes,Files.readAllBytes(file));
        assertEquals(503,assertThrows(GameException.class,()->reload.read(s -> s.players.size())).status());
    }
    @Test void duplicateJsonKeysAndMissingSchemaVersionAreRejected() throws Exception {
        var file=directory.resolve("save.json");
        for (var text : List.of("{\"formatVersion\":1,\"formatVersion\":2}","{\"players\":{},\"attempts\":{},\"progress\":{},\"statistics\":{}}")) {
            Files.writeString(file,text); var reload=new JsonGameRepository(mapper,directory.toString(),new SavedStateValidator(catalog));
            assertNotNull(reload.problem());assertEquals(text,Files.readString(file));
        }
    }
    @Test void completeProgressAndPartialNextChapterRecoverTogether() {
        var restored=new GameFacade(new JsonGameRepository(mapper,directory.toString(),new SavedStateValidator(catalog)),catalog);
        var active=restored.active("demo");assertEquals(activeId,active.id());assertEquals(Attempt.Mode.ASSESSMENT,active.mode());
        assertEquals(2,active.visitedNpcs().size());assertEquals(1,active.acceptedTasks());
        assertEquals(1,restored.summary("demo").completedChapters());assertEquals(1,restored.summary("demo").completedAttempts());
        assertTrue(restored.get("demo",completedId).result().passed());
        assertTrue(restored.map("demo").get(0).displayTitle().contains(catalog.get("c1").title()));
    }
    @Test void duplicateAcceptedRequestAndFinishNeedNoNewDiskWrite() {
        class Failing extends JsonGameRepository {
            boolean fail;
            Failing() { super(mapper,directory.toString(),new SavedStateValidator(catalog)); }
            @Override protected void persist(GameState s) throws IOException { if(fail)throw new IOException("disk unavailable");super.persist(s); }
        }
        var repo=new Failing();var facade=new GameFacade(repo,catalog);
        var original=repo.read(s -> s.attempts.get(activeId).requests.entrySet().iterator().next()); repo.fail=true;
        var retry=facade.answer("demo",activeId,original.getKey().toUpperCase(Locale.ROOT),original.getValue().taskId(),original.getValue().optionId());
        assertEquals(1,retry.acceptedTasks());assertTrue(facade.finish("demo",completedId).result().passed());
        assertEquals(503,assertThrows(GameException.class,()->facade.savePlayer("demo","Новое имя",Player.Avatar.HERO)).status());
        assertEquals("Путешественник",facade.players().get(0).name());
    }
    @ParameterizedTest @ValueSource(ints={1,2,3,4,5,6,7,8,9,10})
    void eachChapterBuildsBothProductsWithValidReferences(int order) throws Exception {
        var definition=mapper.readValue(getClass().getResourceAsStream(String.format("/content/chapter-%02d.json",order)),ChapterDefinition.class);
        var registry=new TaskCreatorRegistry();var chapter=new ChapterBuilder().definition(definition).tasks(definition.tasks().stream().map(registry::prepare).toList()).build();
        assertEquals(order,chapter.order());assertEquals(2,chapter.npcs().size());
        assertInstanceOf(QuizTask.class,chapter.tasks().get(0));assertInstanceOf(QuizTask.class,chapter.tasks().get(1));assertInstanceOf(MapTask.class,chapter.tasks().get(2));
    }
    @Test void builderRejectsNullTextRelativeSourcesBlockedStartAndUnreachableNpc() throws Exception {
        var root=(ObjectNode)mapper.readTree(getClass().getResourceAsStream("/content/chapter-01.json"));
        for (String kind : List.of("null-title","relative-source","blocked-start","unreachable-npc")) {
            var changed=root.deepCopy();
            if (kind.equals("null-title")) changed.putNull("title");
            if (kind.equals("relative-source")) ((ObjectNode)changed.withArray("sources").get(0)).put("url","https:/missing-host");
            if (kind.equals("blocked-start")) changed.withArray("obstacles").add(mapper.createObjectNode().put("x",100).put("y",420).put("w",40).put("h",60).put("kind","cargo"));
            if (kind.equals("unreachable-npc")) changed.withArray("obstacles").add(mapper.createObjectNode().put("x",0).put("y",350).put("w",960).put("h",30).put("kind","cargo"));
            var d=mapper.treeToValue(changed,ChapterDefinition.class);
            assertThrows(IllegalArgumentException.class,()->new ChapterBuilder().definition(d).tasks(d.tasks().stream().map(new TaskCreatorRegistry()::prepare).toList()).build(),kind);
        }
    }
    @Test void blankTaskFieldsAndInvalidUuidAndWhitespaceNameAreRejected() throws Exception {
        var d=mapper.readValue(getClass().getResourceAsStream("/content/chapter-01.json"),ChapterDefinition.class).tasks().get(0);
        var blank=new TaskDefinition(d.kind(),d.id()," ",d.options(),d.correctOptionId(),d.hint(),d.explanation(),d.sourceId());
        assertThrows(IllegalArgumentException.class,()->new TaskCreatorRegistry().prepare(blank));
        assertThrows(IllegalArgumentException.class,()->new TaskCreatorRegistry().prepare(null));
        assertThrows(GameException.class,()->game.savePlayer(null,"\u00a0\u00a0",Player.Avatar.HERO));
        assertEquals(400,assertThrows(GameException.class,()->game.answer("demo",activeId,"1-1-1-1-1","c2-q2","o0")).status());
    }
}
