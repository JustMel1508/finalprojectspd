package edu.historyjourney;

import edu.historyjourney.content.ContentCatalog;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.*;
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
}
