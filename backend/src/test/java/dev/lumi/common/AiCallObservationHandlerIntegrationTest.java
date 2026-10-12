package dev.lumi.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import dev.lumi.IntegrationTest;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Simulates the observation Spring AI's OpenAI models emit around each call and checks that it becomes an
 * ai_call_log row. (The fake models used in tests don't emit observations themselves.)
 */
@IntegrationTest
class AiCallObservationHandlerIntegrationTest {

    @Autowired
    ObservationRegistry observationRegistry;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void clearLog() {
        jdbc.sql("DELETE FROM ai_call_log").update();
    }

    @Test
    void logsOperationModelTokensAndLatency() {
        var context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(new EmbeddingRequest(List.of("hello"), null))
                .provider("openai")
                .build();

        Observation.createNotStarted("test.embedding", () -> context, observationRegistry).observe(() ->
                context.setResponse(new EmbeddingResponse(List.of(new Embedding(new float[] {1f}, 0)),
                        new EmbeddingResponseMetadata("text-embedding-3-small", new DefaultUsage(7, 0)))));

        Map<String, Object> row = jdbc.sql("SELECT * FROM ai_call_log").query().singleRow();
        assertThat(row)
                .containsEntry("operation", "embedding")
                .containsEntry("model", "text-embedding-3-small")
                .containsEntry("input_tokens", 7);
        assertThat((Long) row.get("latency_ms")).isNotNegative();
    }

    @Test
    void logsFailedCallsToo() {
        var context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(new EmbeddingRequest(List.of("hello"), null))
                .provider("openai")
                .build();

        try {
            Observation.createNotStarted("test.embedding", () -> context, observationRegistry).observe(() -> {
                throw new IllegalStateException("API down");
            });
        } catch (IllegalStateException expected) {
            // the failure still reaches the caller
        }

        assertThat(jdbc.sql("SELECT model FROM ai_call_log").query(String.class).single()).isEqualTo("unknown");
    }
}
