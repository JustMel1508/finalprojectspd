package edu.historyjourney.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.historyjourney.model.Chapter;
import edu.historyjourney.model.GameException;
import edu.historyjourney.task.TaskCreatorRegistry;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.*;

@Component
public class ContentCatalog {
    private final List<Chapter> chapters;
    public ContentCatalog(ObjectMapper mapper) throws IOException {
        var result = new ArrayList<Chapter>();
        var registry = new TaskCreatorRegistry();
        for (var resource : new PathMatchingResourcePatternResolver().getResources("classpath*:content/chapter-*.json")) {
            try (var in = resource.getInputStream()) {
                var d = mapper.readValue(in, ChapterDefinition.class);
                result.add(new ChapterBuilder().definition(d).tasks(d.tasks().stream().map(registry::prepare).toList()).build());
            }
        }
        result.sort(Comparator.comparingInt(Chapter::order));
        if (result.isEmpty()) throw new IllegalArgumentException("No chapters");
        var ids = new HashSet<String>();
        for (int i=0; i<result.size(); i++) if (result.get(i).order()!=i+1 || !ids.add(result.get(i).id()))
            throw new IllegalArgumentException("Chapter IDs/order are not unique and consecutive");
        if (getClass().getResource("/static/assets/kazakhstan.svg") == null) throw new IllegalArgumentException("Map resource missing");
        chapters = List.copyOf(result);
    }
    public List<Chapter> all() { return chapters; }
    public Chapter get(String id) { return chapters.stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow(GameException::missing); }
}
