package dev.lumi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Full Spring context against a Testcontainers Postgres (pgvector) with fake AI models. Every integration test
 * uses the same annotation, so they share one cached context and one container.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "lumi.curriculum.ingest-on-startup=false",
        // the fake bag-of-words embeddings give lower scores than real ones
        "lumi.curriculum.similarity-threshold=0.05"
})
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestAiConfiguration.class})
public @interface IntegrationTest {
}
