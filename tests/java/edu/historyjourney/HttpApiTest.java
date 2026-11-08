package edu.historyjourney;

import edu.historyjourney.content.ContentCatalog;
import edu.historyjourney.model.Attempt;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest @AutoConfigureMockMvc
class HttpApiTest {
    static final Path data;
    static {try {data=Files.createTempDirectory("history-http-");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("game.data-dir",()->data.toString());}
    @Autowired MockMvc http;
    @Autowired ContentCatalog catalog;
    @Autowired ObjectMapper mapper;
    @Test void mapHidesTitlesAndAnswersAndRejectsLockedChapter() throws Exception {
        var result=http.perform(get("/api/players/demo/map")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].displayTitle").value("Уровень 1")).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        for(var c:catalog.all()) Assertions.assertFalse(result.contains(c.title()));
        Assertions.assertFalse(result.contains("correctOptionId"));
        http.perform(post("/api/players/demo/attempts").contentType("application/json").content("{\"chapterId\":\"c10\",\"mode\":\"LEARNING\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.action",not(emptyString())));
    }
    @Test void validatesDtoAndStaticResourcesServed() throws Exception {
        for(String name:new String[]{"","   ","a".repeat(41)})http.perform(post("/api/players").contentType("application/json").content("{\"name\":\""+name+"\",\"avatar\":\"HERO\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        http.perform(post("/api/players").contentType("application/json").content("{\"name\":\"Имя\",\"avatar\":\"INVALID\"}")).andExpect(status().isBadRequest());
        http.perform(get("/api/players/missing/map")).andExpect(status().isNotFound());
        http.perform(get("/")).andExpect(status().isOk());
        http.perform(get("/assets/kazakhstan.svg")).andExpect(status().isOk());
        http.perform(get("/content/chapter-01.json")).andExpect(status().isNotFound());
    }
    private JsonNode postJson(String path,Object body,int expected) throws Exception {
        var response=http.perform(post(path).contentType("application/json").content(mapper.writeValueAsBytes(body)))
                .andExpect(status().is(expected)).andReturn().getResponse();
        return mapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }
    @ParameterizedTest @EnumSource(Attempt.Mode.class)
    void fullHttpJourneyChecksModesOwnershipRetriesAndClosedStates(Attempt.Mode mode) throws Exception {
        var p=postJson("/api/players",Map.of("name","HTTP путешественник","avatar","HEROINE"),201).get("id").asText();
        var other=postJson("/api/players",Map.of("name","Другой профиль","avatar","HERO"),201).get("id").asText();
        String base="/api/players/"+p;
        http.perform(put(base).contentType("application/json").content(mapper.writeValueAsBytes(Map.of("name","Обновлённое имя","avatar","HERO"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Обновлённое имя")).andExpect(jsonPath("$.avatar").value("HERO"));
        var a=postJson(base+"/attempts",Map.of("chapterId","c1","mode",mode.name()),201);
        String path=base+"/attempts/"+a.get("id").asText();var chapter=catalog.get("c1");var first=chapter.tasks().get(0);
        postJson(base+"/attempts",Map.of("chapterId","c1","mode",mode.name()),409);
        http.perform(get("/api/players/"+other+"/attempts/"+a.get("id").asText())).andExpect(status().isNotFound());
        postJson(path+"/talk",Map.of("npcId","unknown"),400);
        postJson(path+"/answers",Map.of("requestId",UUID.randomUUID().toString(),"taskId",first.id(),"optionId",first.correctOptionId()),409);
        for(var npc:chapter.npcs()) postJson(path+"/talk",Map.of("npcId",npc.id()),200);
        String wrong=first.options().stream().filter(o->!first.isCorrect(o.id())).findFirst().orElseThrow().id();
        String requestId=UUID.randomUUID().toString();var body=Map.of("requestId",requestId,"taskId",first.id(),"optionId",wrong);
        var answer=postJson(path+"/answers",body,200);
        var retry=postJson(path+"/answers",Map.of("requestId",requestId.toUpperCase(Locale.ROOT),"taskId",first.id(),"optionId",wrong),200);
        Assertions.assertEquals(answer,retry);
        postJson(path+"/answers",Map.of("requestId",requestId,"taskId",first.id(),"optionId",first.correctOptionId()),409);
        if(mode==Attempt.Mode.LEARNING) {
            Assertions.assertTrue(answer.get("feedback").asText().contains(first.hint()));
            Assertions.assertEquals(first.id(),answer.path("currentTask").path("id").asText());
            postJson(path+"/answers",Map.of("requestId",UUID.randomUUID().toString(),"taskId",first.id(),"optionId",first.correctOptionId()),200);
        } else {
            Assertions.assertTrue(answer.get("result").isNull());
            Assertions.assertFalse(answer.toString().contains("\"correct\""));
            Assertions.assertFalse(answer.toString().contains("correctOptionId"));
            postJson(path+"/answers",Map.of("requestId",UUID.randomUUID().toString(),"taskId",first.id(),"optionId",first.correctOptionId()),409);
        }
        for(var task:chapter.tasks().subList(1,3)) postJson(path+"/answers",Map.of("requestId",UUID.randomUUID().toString(),"taskId",task.id(),"optionId",task.correctOptionId()),200);
        var result=postJson(path+"/finish",Map.of(),200);
        Assertions.assertEquals(mode==Attempt.Mode.LEARNING,result.path("result").path("passed").asBoolean());
        Assertions.assertEquals(3,result.path("result").path("statistics").path("tasks").asInt());
        Assertions.assertEquals(1,result.path("result").path("statistics").path("errors").asInt());
        Assertions.assertEquals(result,postJson(path+"/finish",Map.of(),200));
        postJson(path+"/cancel",Map.of(),409);postJson(path+"/talk",Map.of("npcId",chapter.npcs().get(0).id()),409);
        postJson(path+"/answers",Map.of("requestId",UUID.randomUUID().toString(),"taskId",first.id(),"optionId",first.correctOptionId()),409);
        var export=http.perform(get(path+"/export")).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",containsString("attachment"))).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Assertions.assertTrue(export.contains("Игрок: Обновлённое имя"));Assertions.assertTrue(export.contains("Заданий: 3;"));
        Assertions.assertTrue(export.contains("Правильный ответ:"));
        http.perform(get(base+"/summary")).andExpect(jsonPath("$.completedAttempts").value(1))
                .andExpect(jsonPath("$.completedChapters").value(mode==Attempt.Mode.LEARNING?1:0));
        http.perform(get("/api/players/"+other+"/summary")).andExpect(jsonPath("$.completedChapters").value(0));
        var again=postJson(base+"/attempts",Map.of("chapterId","c1","mode",mode.name()),201);
        String cancelled=base+"/attempts/"+again.get("id").asText();
        Assertions.assertEquals("CANCELLED",postJson(cancelled+"/cancel",Map.of(),200).get("state").asText());
        postJson(cancelled+"/cancel",Map.of(),200);postJson(cancelled+"/finish",Map.of(),409);
        http.perform(get(cancelled+"/export")).andExpect(status().isConflict());
    }
}
