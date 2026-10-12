package dev.lumi.common;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.model.observation.ModelObservationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Writes one ai_call_log row for every chat or embedding call, including the ones Spring AI makes on its own
 * (PgVectorStore embedding a query). Spring AI wraps each model call in a Micrometer observation; Spring Boot
 * registers this bean as a handler, so no call site can forget to log.
 */
@Component
class AiCallObservationHandler implements ObservationHandler<ModelObservationContext<?, ?>> {

    private static final Logger log = LoggerFactory.getLogger(AiCallObservationHandler.class);
    private static final String START_NANOS = AiCallObservationHandler.class.getName() + ".start";

    private final JdbcClient jdbc;

    AiCallObservationHandler(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatModelObservationContext || context instanceof EmbeddingModelObservationContext;
    }

    @Override
    public void onStart(ModelObservationContext<?, ?> context) {
        context.put(START_NANOS, System.nanoTime());
    }

    @Override
    public void onStop(ModelObservationContext<?, ?> context) {
        Long start = context.get(START_NANOS);
        long latencyMs = start == null ? 0 : (System.nanoTime() - start) / 1_000_000;
        String model = null;
        Usage usage = null;
        if (context.getResponse() instanceof ChatResponse response && response.getMetadata() != null) {
            model = response.getMetadata().getModel();
            usage = response.getMetadata().getUsage();
        } else if (context.getResponse() instanceof EmbeddingResponse response && response.getMetadata() != null) {
            model = response.getMetadata().getModel();
            usage = response.getMetadata().getUsage();
        }
        if (model == null || model.isBlank()) {
            model = "unknown"; // the call failed before a response arrived
        }
        try {
            jdbc.sql("""
                    INSERT INTO ai_call_log (operation, model, input_tokens, output_tokens, latency_ms)
                    VALUES (:operation, :model, :inputTokens, :outputTokens, :latencyMs)
                    """)
                    .param("operation", context.getOperationMetadata().operationType())
                    .param("model", model)
                    .param("inputTokens", usage == null ? null : usage.getPromptTokens())
                    .param("outputTokens", usage == null ? null : usage.getCompletionTokens())
                    .param("latencyMs", latencyMs)
                    .update();
        } catch (RuntimeException e) {
            log.warn("Could not write ai_call_log row", e); // never break the AI call because of the log
        }
    }
}
