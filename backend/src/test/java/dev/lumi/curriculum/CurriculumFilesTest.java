package dev.lumi.curriculum;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** Guards the real lessons in /curriculum: editing a lesson must not break the format the RAG relies on. */
class CurriculumFilesTest {

    private static final Path CURRICULUM = Path.of("../curriculum");

    private final LessonParser parser = new LessonParser();

    @Test
    void everyLessonParsesWithTheFourStandardSections() throws IOException {
        for (Path file : lessonFiles()) {
            Lesson lesson = parser.parse(Files.readString(file, StandardCharsets.UTF_8));

            assertThat(lesson.slug()).as("slug of %s", file).isEqualTo(file.getFileName().toString().replace(".md", ""));
            assertThat(lesson.sections()).as("sections of %s", file)
                    .extracting(Lesson.Section::name)
                    .containsExactly("Rule", "Examples", "Common mistakes", "Practice");
        }
    }

    @Test
    void coversTheTenSkillsOfTheSyllabus() throws IOException {
        List<String> skills = lessonFiles().stream()
                .map(file -> parser.parse(read(file)).skill())
                .toList();

        assertThat(skills).containsExactlyInAnyOrder(
                "greetings", "numbers-colors", "family", "to-be", "articles", "present-simple",
                "present-simple-3rd", "do-questions", "past-simple", "present-continuous");
    }

    private static List<Path> lessonFiles() throws IOException {
        try (Stream<Path> files = Files.list(CURRICULUM)) {
            return files.filter(f -> f.toString().endsWith(".md")).sorted().toList();
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
