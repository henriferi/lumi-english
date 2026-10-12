package dev.lumi.tutor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.lumi.common.ResourceNotFoundException;
import dev.lumi.curriculum.CurriculumSearch;
import dev.lumi.curriculum.RetrievedChunk;
import dev.lumi.settings.SettingsResponse;
import dev.lumi.settings.SettingsService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

/**
 * One chat turn, step by step: settings → history → retrieval (RAG) → prompt → model → validation → save.
 * The flow is plain Java; the model only writes the reply.
 */
@Service
public class TutorService {

    private final SettingsService settingsService;
    private final ConversationStore conversations;
    private final CurriculumSearch curriculumSearch;
    private final TutorPromptBuilder promptBuilder;
    private final TutorModel model;
    private final TutorProperties properties;

    TutorService(SettingsService settingsService, ConversationStore conversations, CurriculumSearch curriculumSearch,
            TutorPromptBuilder promptBuilder, TutorModel model, TutorProperties properties) {
        this.settingsService = settingsService;
        this.conversations = conversations;
        this.curriculumSearch = curriculumSearch;
        this.promptBuilder = promptBuilder;
        this.model = model;
        this.properties = properties;
    }

    public ChatResponse chat(ChatRequest request) {
        String text = request.text().strip();
        SettingsResponse settings = settingsService.get();
        long conversationId = resolveConversation(request.conversationId());
        List<Message> history = conversations.recentMessages(conversationId, properties.historySize());

        List<RetrievedChunk> chunks = curriculumSearch.search(text, settings.level());
        String systemPrompt = promptBuilder.systemPrompt(settings, chunks);

        Map<String, String> titlesBySlug = new LinkedHashMap<>();
        chunks.forEach(chunk -> titlesBySlug.putIfAbsent(chunk.slug(), chunk.title()));

        Optional<TutorReply> answer = model.ask(systemPrompt, history, text);
        boolean fallback = answer.isEmpty();
        TutorReply reply = answer.map(r -> r.sanitized(titlesBySlug.keySet())).orElse(TutorReply.FALLBACK);

        long messageId = conversations.saveExchange(conversationId, text, reply, new ChatTrace(chunks, fallback));
        List<ChatResponse.Source> sources = reply.sources().stream()
                .map(slug -> new ChatResponse.Source(slug, titlesBySlug.get(slug)))
                .toList();
        return new ChatResponse(conversationId, messageId, reply, sources, fallback);
    }

    private long resolveConversation(Long conversationId) {
        if (conversationId == null) {
            return conversations.createConversation();
        }
        if (!conversations.exists(conversationId)) {
            throw new ResourceNotFoundException("Conversation %d not found".formatted(conversationId));
        }
        return conversationId;
    }
}
