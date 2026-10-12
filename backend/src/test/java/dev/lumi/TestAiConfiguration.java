package dev.lumi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the OpenAI models in integration tests: tests never call the real API. */
@TestConfiguration(proxyBeanMethods = false)
public class TestAiConfiguration {

    @Bean
    @Primary
    FakeEmbeddingModel fakeEmbeddingModel() {
        return new FakeEmbeddingModel();
    }

    @Bean
    @Primary
    FakeChatModel fakeChatModel() {
        return new FakeChatModel();
    }
}
