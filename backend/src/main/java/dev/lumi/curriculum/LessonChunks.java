package dev.lumi.curriculum;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;

/** Turns a lesson into vector store documents: one per section, with the lesson's metadata. */
final class LessonChunks {

    static final String SOURCE = "source";
    static final String SLUG = "slug";
    static final String TITLE = "title";
    static final String LEVEL = "level";
    static final String LEVEL_RANK = "levelRank";
    static final String SKILL = "skill";
    static final String SECTION = "section";

    private LessonChunks() {
    }

    /**
     * The lesson title and section name go into the text too: they carry meaning ("Past simple", "Common mistakes")
     * and make the embedding of a short section much more specific.
     */
    static List<Document> toDocuments(Lesson lesson, String source) {
        return lesson.sections().stream()
                .map(section -> new Document(
                        "%s — %s\n\n%s".formatted(lesson.title(), section.name(), section.content()),
                        Map.<String, Object>of(
                                SOURCE, source,
                                SLUG, lesson.slug(),
                                TITLE, lesson.title(),
                                LEVEL, lesson.level().name(),
                                LEVEL_RANK, lesson.level().rank(),
                                SKILL, lesson.skill(),
                                SECTION, section.name())))
                .toList();
    }
}
