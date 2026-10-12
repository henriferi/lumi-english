package dev.lumi.tutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

/**
 * Calls the chat model and turns its answer into a valid {@link TutorReply}. The JSON schema is enforced by OpenAI
 * (provider structured output) and the result is validated again here; an invalid answer gets one more attempt.
 */
@Component
class TutorModel {

    static final int MAX_ATTEMPTS = 2;

    private static final Logger log = LoggerFactory.getLogger(TutorModel.class);

    private final ChatClient chatClient;
    private final Validator validator;

    TutorModel(ChatClient.Builder chatClientBuilder, Validator validator) {
        this.chatClient = chatClientBuilder.build();
        this.validator = validator;
    }

    /** Returns empty when every attempt failed; the caller then answers with a friendly fallback. */
    Optional<TutorReply> ask(String systemPrompt, List<Message> history, String learnerText) {
        // Plain Message objects, not .system(String)/.user(String): those are rendered as templates, and a learner
        // typing "{" would break the request.
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.addAll(history);
        messages.add(new UserMessage(learnerText));

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                TutorReply reply = chatClient.prompt()
                        .messages(messages)
                        .call()
                        .entity(TutorReply.class, spec -> spec.useProviderStructuredOutput());
                if (reply == null) {
                    log.warn("Attempt {}/{}: model returned no content", attempt, MAX_ATTEMPTS);
                    continue;
                }
                Set<ConstraintViolation<TutorReply>> violations = validator.validate(reply);
                if (violations.isEmpty()) {
                    return Optional.of(reply);
                }
                log.warn("Attempt {}/{}: invalid tutor reply: {}", attempt, MAX_ATTEMPTS, violations);
            } catch (RuntimeException e) {
                log.warn("Attempt {}/{}: tutor call failed: {}", attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }
        return Optional.empty();
    }
}
