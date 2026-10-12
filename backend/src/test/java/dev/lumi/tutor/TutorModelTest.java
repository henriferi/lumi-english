package dev.lumi.tutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import dev.lumi.FakeChatModel;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;

/** The model call with a scripted ChatModel: parsing, validation, retry and fallback. No OpenAI involved. */
class TutorModelTest {

    private static final String VALID = """
            {"reply": "Nice! What did you buy?", "explanationPt": "Go vira went no passado.",
             "correction": {"original": "Yesterday I go.", "corrected": "Yesterday I went."},
             "sources": ["a1-past-simple"]}
            """;

    private final FakeChatModel chatModel = new FakeChatModel();
    private final TutorModel model = new TutorModel(ChatClient.builder(chatModel),
            Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void mapsTheJsonAnswerToATutorReply() {
        chatModel.willAnswer(VALID);

        Optional<TutorReply> reply = model.ask("system", List.of(), "Yesterday I go.");

        assertThat(reply).hasValueSatisfying(r -> {
            assertThat(r.reply()).isEqualTo("Nice! What did you buy?");
            assertThat(r.correction().corrected()).isEqualTo("Yesterday I went.");
            assertThat(r.sources()).containsExactly("a1-past-simple");
        });
        assertThat(chatModel.prompts()).hasSize(1);
    }

    @Test
    void sendsSystemPromptThenHistoryThenTheNewMessage() {
        chatModel.willAnswer(VALID);
        List<Message> history = List.of(new UserMessage("Hi"), new AssistantMessage("Hello! How are you?"));

        model.ask("system prompt", history, "I am fine.");

        assertThat(chatModel.prompts().getFirst().getInstructions())
                .extracting(Message::getMessageType)
                .containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT, MessageType.USER);
    }

    @Test
    void retriesOnceWhenTheAnswerIsNotValidJson() {
        chatModel.willAnswer("this is not json", VALID);

        Optional<TutorReply> reply = model.ask("system", List.of(), "Yesterday I go.");

        assertThat(reply).isPresent();
        assertThat(chatModel.prompts()).hasSize(2);
    }

    @Test
    void retriesWhenTheJsonBreaksTheValidationRules() {
        chatModel.willAnswer("""
                {"reply": " ", "explanationPt": null, "correction": null, "sources": []}
                """, VALID);

        Optional<TutorReply> reply = model.ask("system", List.of(), "Hello");

        assertThat(reply).hasValueSatisfying(r -> assertThat(r.reply()).isNotBlank());
        assertThat(chatModel.prompts()).hasSize(2);
    }

    @Test
    void givesUpAfterTwoFailedAttempts() {
        chatModel.willAnswer("not json", "still not json", VALID);

        Optional<TutorReply> reply = model.ask("system", List.of(), "Hello");

        assertThat(reply).isEmpty();
        assertThat(chatModel.prompts()).hasSize(TutorModel.MAX_ATTEMPTS);
    }

    @Test
    void learnerTextWithBracesIsSentAsIs() {
        chatModel.willAnswer(VALID);

        model.ask("system", List.of(), "What is {this}?");

        assertThat(chatModel.prompts().getFirst().getUserMessage().getText()).isEqualTo("What is {this}?");
    }
}
