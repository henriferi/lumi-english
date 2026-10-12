package dev.lumi.curriculum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.lumi.settings.Level;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * Parses a lesson file: YAML front matter between "---" lines, then the body split by "## " headings.
 * Splitting by section (not by fixed size) keeps each chunk a complete idea: the rule, the examples, ...
 */
@Component
public class LessonParser {

    private static final String FRONT_MATTER_DELIMITER = "---";
    private static final String SECTION_PREFIX = "## ";

    public Lesson parse(String markdown) {
        List<String> lines = markdown.replace("\r\n", "\n").lines().toList();
        if (lines.isEmpty() || !lines.getFirst().strip().equals(FRONT_MATTER_DELIMITER)) {
            throw new InvalidLessonException("Lesson must start with a '---' front matter block");
        }
        int end = indexOfClosingDelimiter(lines);
        Map<String, Object> frontMatter = parseFrontMatter(String.join("\n", lines.subList(1, end)));
        List<Lesson.Section> sections = parseSections(lines.subList(end + 1, lines.size()));

        String slug = required(frontMatter, "slug");
        if (sections.isEmpty()) {
            throw new InvalidLessonException("Lesson '%s' has no '## ' sections".formatted(slug));
        }
        return new Lesson(
                slug,
                required(frontMatter, "title"),
                required(frontMatter, "title_pt"),
                parseLevel(required(frontMatter, "level")),
                required(frontMatter, "skill"),
                sections);
    }

    private static int indexOfClosingDelimiter(List<String> lines) {
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).strip().equals(FRONT_MATTER_DELIMITER)) {
                return i;
            }
        }
        throw new InvalidLessonException("Front matter is not closed with '---'");
    }

    private static Map<String, Object> parseFrontMatter(String yaml) {
        Object loaded = new Yaml().load(yaml);
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new InvalidLessonException("Front matter must be a YAML map");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> frontMatter = (Map<String, Object>) map;
        return frontMatter;
    }

    private static List<Lesson.Section> parseSections(List<String> body) {
        List<Lesson.Section> sections = new ArrayList<>();
        String name = null;
        StringBuilder content = new StringBuilder();
        for (String line : body) {
            if (line.startsWith(SECTION_PREFIX)) {
                addSection(sections, name, content);
                name = line.substring(SECTION_PREFIX.length()).strip();
                content.setLength(0);
            } else if (name != null) {
                content.append(line).append('\n');
            }
        }
        addSection(sections, name, content);
        return sections;
    }

    private static void addSection(List<Lesson.Section> sections, String name, StringBuilder content) {
        String text = content.toString().strip();
        if (name != null && !text.isEmpty()) {
            sections.add(new Lesson.Section(name, text));
        }
    }

    private static String required(Map<String, Object> frontMatter, String key) {
        Object value = frontMatter.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new InvalidLessonException("Front matter field '%s' is required".formatted(key));
        }
        return value.toString().strip();
    }

    private static Level parseLevel(String value) {
        try {
            return Level.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidLessonException("Unknown level '%s' (use PRE_A1, A1 or A2)".formatted(value));
        }
    }
}
