package dev.lumi.tutor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import dev.lumi.curriculum.RetrievedChunk;
import dev.lumi.settings.AgeBand;
import dev.lumi.settings.Level;
import dev.lumi.settings.SettingsResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Builds the system prompt from the templates in resources/prompts: the persona depends on the age group, the use
 * of Portuguese on the level, and the retrieved lesson sections are pasted in as context (the "A" in RAG).
 */
@Component
public class TutorPromptBuilder {

    static final String NO_CONTEXT = "(No lesson matched this message. Do not invent grammar rules.)";

    private final PromptTemplate systemTemplate;
    private final Map<AgeBand, String> personas = new EnumMap<>(AgeBand.class);
    private final Map<Level, String> languagePolicies = new EnumMap<>(Level.class);

    public TutorPromptBuilder() {
        this.systemTemplate = new PromptTemplate(read("tutor-system.st"));
        for (AgeBand ageBand : AgeBand.values()) {
            personas.put(ageBand, read("age-" + slug(ageBand.name()) + ".st"));
        }
        for (Level level : Level.values()) {
            languagePolicies.put(level, read("level-" + slug(level.name()) + ".st"));
        }
    }

    public String systemPrompt(SettingsResponse settings, List<RetrievedChunk> chunks) {
        return systemTemplate.render(Map.of(
                "name", settings.name(),
                "level", settings.level().name(),
                "persona", personas.get(settings.ageBand()),
                "language", languagePolicies.get(settings.level()),
                "context", formatContext(chunks)));
    }

    private static String formatContext(List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return NO_CONTEXT;
        }
        return chunks.stream()
                .map(chunk -> "[slug: %s]\n%s".formatted(chunk.slug(), chunk.content()))
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private static String slug(String enumName) {
        return enumName.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String read(String fileName) {
        try {
            return new ClassPathResource("prompts/" + fileName).getContentAsString(StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Missing prompt template: prompts/" + fileName, e);
        }
    }
}
