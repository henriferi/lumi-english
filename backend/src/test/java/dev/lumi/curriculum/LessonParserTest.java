package dev.lumi.curriculum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.lumi.settings.Level;
import org.junit.jupiter.api.Test;

class LessonParserTest {

    private final LessonParser parser = new LessonParser();

    private static final String LESSON = """
            ---
            slug: a1-past-simple
            title: "Past simple — regular and irregular verbs"
            title_pt: "Passado simples"
            level: A1            # PRE_A1 | A1 | A2
            skill: past-simple
            ---

            ## Rule
            go → went.
            Em português: alguns verbos mudam de forma.

            ## Examples
            - Yesterday I **went** to the park.
            """;

    @Test
    void readsFrontMatter() {
        Lesson lesson = parser.parse(LESSON);

        assertThat(lesson.slug()).isEqualTo("a1-past-simple");
        assertThat(lesson.title()).isEqualTo("Past simple — regular and irregular verbs");
        assertThat(lesson.titlePt()).isEqualTo("Passado simples");
        assertThat(lesson.level()).isEqualTo(Level.A1);
        assertThat(lesson.skill()).isEqualTo("past-simple");
    }

    @Test
    void splitsTheBodyIntoOneSectionPerHeading() {
        Lesson lesson = parser.parse(LESSON);

        assertThat(lesson.sections()).containsExactly(
                new Lesson.Section("Rule", "go → went.\nEm português: alguns verbos mudam de forma."),
                new Lesson.Section("Examples", "- Yesterday I **went** to the park."));
    }

    @Test
    void acceptsWindowsLineEndings() {
        Lesson lesson = parser.parse(LESSON.replace("\n", "\r\n"));

        assertThat(lesson.sections()).hasSize(2);
        assertThat(lesson.sections().getFirst().content()).doesNotContain("\r");
    }

    @Test
    void skipsEmptySections() {
        Lesson lesson = parser.parse(LESSON + "\n## Practice\n\n");

        assertThat(lesson.sections()).extracting(Lesson.Section::name).containsExactly("Rule", "Examples");
    }

    @Test
    void rejectsMissingFrontMatter() {
        assertThatThrownBy(() -> parser.parse("## Rule\nsomething"))
                .isInstanceOf(InvalidLessonException.class)
                .hasMessageContaining("front matter");
    }

    @Test
    void rejectsMissingRequiredField() {
        assertThatThrownBy(() -> parser.parse(LESSON.replace("skill: past-simple\n", "")))
                .isInstanceOf(InvalidLessonException.class)
                .hasMessageContaining("'skill'");
    }

    @Test
    void rejectsUnknownLevel() {
        assertThatThrownBy(() -> parser.parse(LESSON.replace("level: A1", "level: B2")))
                .isInstanceOf(InvalidLessonException.class)
                .hasMessageContaining("B2");
    }

    @Test
    void rejectsLessonWithoutSections() {
        String noSections = LESSON.substring(0, LESSON.indexOf("## Rule"));

        assertThatThrownBy(() -> parser.parse(noSections))
                .isInstanceOf(InvalidLessonException.class)
                .hasMessageContaining("no '## ' sections");
    }
}
