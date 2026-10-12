package dev.lumi.curriculum;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import dev.lumi.IntegrationTest;
import dev.lumi.settings.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Ingestion + vector search against a real pgvector (Testcontainers), with fake embeddings. */
@IntegrationTest
class CurriculumIngestionIntegrationTest {

    private static final int LESSONS = 10;
    private static final int SECTIONS_PER_LESSON = 4;

    @Autowired
    CurriculumIngestionService ingestion;

    @Autowired
    CurriculumSearch search;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void startFromAnEmptyIndex() {
        jdbc.sql("DELETE FROM vector_store").update();
        jdbc.sql("DELETE FROM curriculum_file").update();
    }

    @Test
    void indexesEverySectionOfEveryLesson() {
        var report = ingestion.ingestAll();

        assertThat(report.indexed()).hasSize(LESSONS);
        assertThat(report.failed()).isEmpty();
        assertThat(count("vector_store")).isEqualTo(LESSONS * SECTIONS_PER_LESSON);
        assertThat(jdbc.sql("SELECT name_pt FROM skill WHERE tag = 'past-simple'").query(String.class).single())
                .isEqualTo("Passado simples — verbos regulares e irregulares");
    }

    @Test
    void secondRunSkipsUnchangedFiles() {
        ingestion.ingestAll();

        var report = ingestion.ingestAll();

        assertThat(report.indexed()).isEmpty();
        assertThat(report.unchanged()).hasSize(LESSONS);
        assertThat(count("vector_store")).isEqualTo(LESSONS * SECTIONS_PER_LESSON);
    }

    @Test
    void changedFileReplacesItsOldChunks() {
        ingestion.ingestAll();
        jdbc.sql("UPDATE curriculum_file SET content_hash = 'outdated' WHERE path = 'a1-past-simple.md'").update();

        var report = ingestion.ingestAll();

        assertThat(report.indexed()).containsExactly("a1-past-simple.md");
        assertThat(count("vector_store")).isEqualTo(LESSONS * SECTIONS_PER_LESSON); // replaced, not duplicated
    }

    @Test
    void fileThatNoLongerExistsIsRemoved() {
        ingestion.ingestAll();
        jdbc.sql("INSERT INTO curriculum_file (path, content_hash) VALUES ('deleted-lesson.md', 'x')").update();

        var report = ingestion.ingestAll();

        assertThat(report.removed()).containsExactly("deleted-lesson.md");
        assertThat(count("curriculum_file")).isEqualTo(LESSONS);
    }

    @Test
    void searchFindsTheMostRelevantLesson() {
        ingestion.ingestAll();

        List<RetrievedChunk> chunks = search.search("Yesterday I go to the park and I see a dog", Level.A1);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.getFirst().slug()).isEqualTo("a1-past-simple");
        assertThat(chunks).isSortedAccordingTo((a, b) -> Double.compare(b.score(), a.score()));
    }

    @Test
    void searchNeverReturnsLessonsAboveTheLearnerLevel() {
        ingestion.ingestAll();

        List<RetrievedChunk> chunks = search.search("Yesterday I went to the park. Good morning, my name is Ana.",
                Level.PRE_A1);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).extracting(RetrievedChunk::slug).allMatch(slug -> slug.startsWith("pre-a1-"));
    }

    private int count(String table) {
        return jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single();
    }
}
