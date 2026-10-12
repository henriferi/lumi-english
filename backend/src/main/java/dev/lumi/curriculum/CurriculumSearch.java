package dev.lumi.curriculum;

import java.util.List;
import java.util.Map;

import dev.lumi.settings.Level;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/** The "R" in RAG: finds the lesson sections closest to the learner's message, up to the learner's level. */
@Service
public class CurriculumSearch {

    private final VectorStore vectorStore;
    private final CurriculumProperties properties;

    CurriculumSearch(VectorStore vectorStore, CurriculumProperties properties) {
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    public List<RetrievedChunk> search(String query, Level learnerLevel) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(properties.topK())
                .similarityThreshold(properties.similarityThreshold())
                .filterExpression("%s <= %d".formatted(LessonChunks.LEVEL_RANK, learnerLevel.rank()))
                .build();
        return vectorStore.similaritySearch(request).stream()
                .map(CurriculumSearch::toChunk)
                .toList();
    }

    private static RetrievedChunk toChunk(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new RetrievedChunk(
                (String) metadata.get(LessonChunks.SLUG),
                (String) metadata.get(LessonChunks.TITLE),
                (String) metadata.get(LessonChunks.SECTION),
                document.getText(),
                document.getScore() == null ? 0 : document.getScore());
    }
}
