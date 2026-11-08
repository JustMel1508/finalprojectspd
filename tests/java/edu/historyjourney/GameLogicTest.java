package edu.historyjourney;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.historyjourney.content.*;
import edu.historyjourney.event.*;
import edu.historyjourney.model.*;
import edu.historyjourney.result.*;
import edu.historyjourney.service.GameFacade;
import edu.historyjourney.storage.JsonGameRepository;
import edu.historyjourney.task.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GameLogicTest {
    @TempDir Path directory;
    ObjectMapper mapper; ContentCatalog catalog; JsonGameRepository repository; GameFacade game;
    @BeforeEach void setup() throws Exception {
        mapper=new ObjectMapper().findAndRegisterModules(); catalog=new ContentCatalog(mapper);
        repository=new JsonGameRepository(mapper,directory.toString()); game=new GameFacade(repository,catalog);
    }
    GameFacade.AttemptView start(int n,Attempt.Mode mode) { return game.start("demo","c"+n,mode); }
    GameFacade.AttemptView talk(GameFacade.AttemptView a) {
        for(var n:catalog.get(a.chapterId()).npcs()) a=game.talk("demo",a.id(),n.id()); return a;
    }
    GameFacade.AttemptView complete(GameFacade.AttemptView a,boolean wrongFirst) {
        a=talk(a); int index=0;
        for(var t:catalog.get(a.chapterId()).tasks()) {
            String option=wrongFirst&&index++==0?t.options().stream().filter(o->!t.isCorrect(o.id())).findFirst().orElseThrow().id():t.correctOptionId();
            a=game.answer("demo",a.id(),UUID.randomUUID().toString(),t.id(),option);
        }
        return game.finish("demo",a.id());
    }
    @ParameterizedTest @EnumSource(Attempt.Mode.class)
    void allTenChaptersAndNineUnlocks(Attempt.Mode mode) {
        assertEquals(10,catalog.all().size());
        for(int i=1;i<=10;i++) {
            var before=game.map("demo"); assertEquals("AVAILABLE",before.get(i-1).status());
            if(i<10) assertEquals("LOCKED",before.get(i).status());
            var a=complete(start(i,mode),false);
            assertTrue(a.result().passed()); assertEquals(3,a.result().statistics().tasks());
            assertEquals(3,a.result().statistics().correct()); assertEquals(1,a.result().statistics().mapCorrect());
            assertEquals(i,game.summary("demo").completedChapters());
            assertTrue(game.export("demo",a.id()).contains("Правильный ответ:"));
        }
        var s=game.summary("demo"); assertTrue(s.finished()); assertEquals(10,s.completedAttempts());
        assertTrue(game.exportSummary("demo").contains("Пройдено: 10 / 10"));
    }
    @Test void learningHintRetryCountsOneTaskAndIdempotentRequest() {
        var a=talk(start(1,Attempt.Mode.LEARNING)); var t=catalog.get("c1").tasks().get(0);var request=UUID.randomUUID().toString();
        String wrong=t.options().stream().filter(o->!t.isCorrect(o.id())).findFirst().orElseThrow().id();
        a=game.answer("demo",a.id(),request,t.id(),wrong); assertTrue(a.feedback().contains(t.hint())); assertEquals(t.id(),a.currentTask().id());
        var retry=game.answer("demo",a.id(),request,t.id(),wrong); assertEquals(a,retry);
        String id=a.id(); assertThrows(GameException.class,()->game.answer("demo",id,request,t.id(),t.correctOptionId()));
        a=game.answer("demo",id,UUID.randomUUID().toString(),t.id(),t.correctOptionId());
        for(var task:catalog.get("c1").tasks().subList(1,3)) a=game.answer("demo",id,UUID.randomUUID().toString(),task.id(),task.correctOptionId());
        a=game.finish("demo",id);var s=a.result().statistics();assertEquals(3,s.tasks());assertEquals(4,s.tries());assertEquals(1,s.errors());assertTrue(a.result().passed());
    }
    @Test void assessmentHidesCorrectnessAndSavesFailure() throws Exception {
        var a=talk(start(1,Attempt.Mode.ASSESSMENT));var t=catalog.get("c1").tasks().get(0);
        a=game.answer("demo",a.id(),UUID.randomUUID().toString(),t.id(),"o2");
        String json=mapper.writeValueAsString(a);assertFalse(json.contains("correctOptionId"));assertNull(a.result());assertFalse(a.feedback().contains(t.explanation()));
        for(var task:catalog.get("c1").tasks().subList(1,3)) a=game.answer("demo",a.id(),UUID.randomUUID().toString(),task.id(),task.correctOptionId());
        a=game.finish("demo",a.id());assertFalse(a.result().passed());assertEquals("Уровень 1",a.displayTitle());
        assertEquals(1,game.summary("demo").failedAttempts());assertEquals(0,game.summary("demo").completedChapters());
    }
    @Test void replayFinishAndObserverDeduplication() {
        var a=complete(start(1,Attempt.Mode.ASSESSMENT),false);game.finish("demo",a.id());
        assertEquals(1,game.summary("demo").completedAttempts());
        var failed=complete(start(1,Attempt.Mode.ASSESSMENT),true);assertFalse(failed.result().passed());
        assertTrue(failed.displayTitle().contains(catalog.get("c1").title()));assertEquals("AVAILABLE",game.map("demo").get(1).status());
        assertEquals(2,game.summary("demo").completedAttempts());assertEquals(1,game.summary("demo").completedChapters());
        var publisher=new ChapterCompletedPublisher();publisher.subscribe(new ProgressObserver());publisher.subscribe(new CompletionStatisticsObserver());
        repository.transaction(s->{var e=new ChapterCompleted(a.id(),"demo","c1",true);publisher.publish(e,s);publisher.publish(e,s);return null;});
        assertEquals(2,game.summary("demo").completedAttempts());
    }
    @Test void saveReloadProfileActiveTalksAndAnswers() {
        game.savePlayer("demo","Қазақ саяхатшысы",Player.Avatar.HERO);var a=talk(start(1,Attempt.Mode.LEARNING));var t=catalog.get("c1").tasks().get(0);
        a=game.answer("demo",a.id(),UUID.randomUUID().toString(),t.id(),t.correctOptionId());
        var reload=new GameFacade(new JsonGameRepository(mapper,directory.toString()),catalog);
        assertEquals(a.id(),reload.active("demo").id());assertEquals(2,reload.active("demo").visitedNpcs().size());assertEquals(1,reload.active("demo").acceptedTasks());
        assertEquals("Қазақ саяхатшысы",reload.players().get(0).name());assertEquals(Player.Avatar.HERO,reload.players().get(0).avatar());
    }
    @Test void cancellationAndIndependentProfile() {
        complete(start(1,Attempt.Mode.LEARNING),false);var a=start(2,Attempt.Mode.ASSESSMENT);game.cancel("demo",a.id());game.cancel("demo",a.id());
        assertEquals(1,game.summary("demo").completedChapters());assertEquals(1,game.summary("demo").cancelledAttempts());
        var other=game.savePlayer(null,"Другой",Player.Avatar.HEROINE);assertEquals(0,game.summary(other.id()).completedChapters());
        assertThrows(GameException.class,()->game.get(other.id(),a.id()));assertThrows(GameException.class,()->game.finish("demo",a.id()));
    }
    @Test void validationAndIllegalStages() {
        for(var name:List.of("","   ","x".repeat(41),"Имя\n")) assertThrows(GameException.class,()->game.savePlayer(null,name,Player.Avatar.HERO));
        assertThrows(GameException.class,()->game.savePlayer(null,"Имя",null));assertThrows(GameException.class,()->start(2,Attempt.Mode.LEARNING));
        assertThrows(GameException.class,()->game.start("demo","unknown",Attempt.Mode.LEARNING));
        var a=start(1,Attempt.Mode.LEARNING);var t=catalog.get("c1").tasks().get(0);
        assertThrows(GameException.class,()->start(1,Attempt.Mode.LEARNING));assertThrows(GameException.class,()->game.finish("demo",a.id()));
        assertThrows(GameException.class,()->game.answer("demo",a.id(),UUID.randomUUID().toString(),t.id(),t.correctOptionId()));
        talk(a);assertThrows(GameException.class,()->game.answer("demo",a.id(),UUID.randomUUID().toString(),t.id(),"unknown"));
        assertThrows(GameException.class,()->game.answer("demo",a.id(),UUID.randomUUID().toString(),"c1-map","shu"));
        assertThrows(GameException.class,()->game.answer("demo",a.id(),"invalid",t.id(),t.correctOptionId()));
    }
    @Test void writeFailureRollsBackCompletionAndObservers() {
        class Failing extends JsonGameRepository { boolean fail;Failing(){super(mapper,directory.toString());} @Override protected void persist(GameState s)throws IOException{if(fail)throw new IOException("simulated disk full");super.persist(s);} }
        var repo=new Failing();var facade=new GameFacade(repo,catalog);var a=facade.start("demo","c1",Attempt.Mode.ASSESSMENT);
        for(var n:catalog.get("c1").npcs())facade.talk("demo",a.id(),n.id());
        for(var t:catalog.get("c1").tasks())facade.answer("demo",a.id(),UUID.randomUUID().toString(),t.id(),t.correctOptionId());
        repo.fail=true;var e=assertThrows(GameException.class,()->facade.finish("demo",a.id()));assertEquals(503,e.status());
        assertEquals(Attempt.State.ACTIVE,facade.get("demo",a.id()).state());assertEquals(0,facade.summary("demo").completedAttempts());
        var reloaded=new GameFacade(new JsonGameRepository(mapper,directory.toString()),catalog);assertEquals(Attempt.State.ACTIVE,reloaded.get("demo",a.id()).state());
        repo.fail=false;facade.finish("demo",a.id());assertEquals(1,facade.summary("demo").completedAttempts());
    }
    @Test void corruptSavePreservedAndActionable() throws Exception {
        Files.writeString(directory.resolve("save.json"),"{broken");var repo=new JsonGameRepository(mapper,directory.toString());
        assertNotNull(repo.problem());assertTrue(repo.problem().contains("переименуйте"));assertEquals("{broken",Files.readString(directory.resolve("save.json")));
        assertThrows(GameException.class,()->repo.transaction(s->null));
    }
    @Test void builderAndFactoryProductsAndCoordinates() throws Exception {
        assertThrows(IllegalArgumentException.class,()->new ChapterBuilder().build());
        for(var c:catalog.all()) {
            assertEquals(2,c.npcs().size());assertEquals(3,c.tasks().size());assertInstanceOf(QuizTask.class,c.tasks().get(0));assertInstanceOf(MapTask.class,c.tasks().get(2));
            for(var n:c.npcs()) assertTrue(c.sources().stream().anyMatch(s->s.id().equals(n.sourceId())));
        }
        var d=mapper.readValue(getClass().getResourceAsStream("/content/chapter-01.json"),ChapterDefinition.class);
        var registry=new TaskCreatorRegistry();assertEquals("c1",new ChapterBuilder().definition(d).tasks(d.tasks().stream().map(registry::prepare).toList()).build().id());
        var invalid=new TaskDefinition("MAP","bad","bad",List.of(new Task.Option("a","a",200d,0d),new Task.Option("b","b",0d,0d)),"a","hint","explain","S01");
        assertThrows(IllegalArgumentException.class,()->registry.prepare(invalid));
    }
    @Test void bothVisitorsDoubleDispatch() {
        var stats=new StatisticsVisitor();var text=new TextReportVisitor();
        List<TaskResult> details=List.of(new QuizResult("q","Вопрос","А","А",true,2,1,"Пояснение","S1"),new MapTaskResult("m","Место","Б","В",false,1,1,"География","S1",55,51));
        details.forEach(r->{r.accept(stats);r.accept(text);});assertEquals(2,stats.summary().tasks());assertEquals(3,stats.summary().tries());assertEquals(2,stats.summary().errors());
        assertTrue(text.text().contains("Вопрос: Вопрос"));assertTrue(text.text().contains("Карта: Место"));assertTrue(text.text().contains("55.0, 51.0"));
    }
    @Test void everyNpcReachableFromSafeStartAcrossAllTenScenes() {
        for(var c:catalog.all()) {
            var reached=new HashSet<String>();var queue=new ArrayDeque<int[]>();var seen=new HashSet<String>();queue.add(new int[]{120,440});
            while(!queue.isEmpty()) {
                var p=queue.remove();String key=p[0]+":"+p[1];if(!seen.add(key))continue;
                if(p[0]<25||p[0]>935||p[1]<170||p[1]>510||c.obstacles().stream().anyMatch(o->p[0]>o.x()-13&&p[0]<o.x()+o.w()+13&&p[1]>o.y()-10&&p[1]<o.y()+o.h()+10))continue;
                for(var n:c.npcs())if(Math.hypot(n.x()-p[0],n.y()-p[1])<60)reached.add(n.id());
                for(var d:new int[][]{{10,0},{-10,0},{0,10},{0,-10}})if(!seen.contains((p[0]+d[0])+":"+(p[1]+d[1])))queue.add(new int[]{p[0]+d[0],p[1]+d[1]});
            }
            assertEquals(c.npcs().size(),reached.size(),"Unreachable NPC in "+c.id());
        }
    }
    @Test void concurrentRequestsSerialized() throws Exception {
        try(var ignored=new AutoCloseable(){public void close(){}}) {
            var threads=new ArrayList<Thread>();for(int i=0;i<8;i++){final int n=i;threads.add(new Thread(()->game.savePlayer(null,"Игрок "+n,Player.Avatar.HERO)));}
            threads.forEach(Thread::start);for(var t:threads)t.join();assertEquals(9,game.players().size());
            var reload=new JsonGameRepository(mapper,directory.toString());assertEquals(9,reload.read(s->s.players.size()).intValue());
        }
    }
}
