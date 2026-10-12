package dev.lumi.tutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;

import com.jayway.jsonpath.JsonPath;
import dev.lumi.FakeChatModel;
import dev.lumi.IntegrationTest;
import dev.lumi.curriculum.CurriculumIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** POST /api/chat end to end: real database and retrieval, scripted chat model. */
@IntegrationTest
class TutorChatIntegrationTest {

    private static final String REPLY_WITH_CORRECTION = """
            {"reply": "Nice! What did you buy?", "explanationPt": "Go vira went no passado.",
             "correction": {"original": "Yesterday I go to the supermarket.",
                            "corrected": "Yesterday I went to the supermarket."},
             "sources": ["a1-past-simple", "invented-lesson"]}
            """;

    @Autowired
    MockMvcTester mvc;

    @Autowired
    FakeChatModel chatModel;

    @Autowired
    CurriculumIngestionService ingestion;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void setUp() {
        chatModel.reset();
        ingestion.ingestAll();
        jdbc.sql("UPDATE learner_settings SET name = 'Ana', age_band = 'ADULT', level = 'A1'").update();
    }

    @Test
    void answersWithCorrectionAndOnlyRetrievedSources() {
        chatModel.willAnswer(REPLY_WITH_CORRECTION);

        assertThat(send(null, "Yesterday I go to the supermarket."))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"reply": {"reply": "Nice! What did you buy?",
                                   "correction": {"corrected": "Yesterday I went to the supermarket."},
                                   "sources": ["a1-past-simple"]},
                         "sources": [{"slug": "a1-past-simple",
                                      "title": "Past simple — regular and irregular verbs"}],
                         "fallback": false}
                        """);
    }

    @Test
    void sendsRetrievedLessonsToTheModelInTheSystemPrompt() {
        chatModel.willAnswer(REPLY_WITH_CORRECTION);

        send(null, "Yesterday I go to the supermarket.");

        String systemPrompt = chatModel.prompts().getFirst().getSystemMessage().getText();
        assertThat(systemPrompt).contains("[slug: a1-past-simple]").contains("The learner's name is Ana");
    }

    @Test
    void savesBothMessagesWithTheTrace() {
        chatModel.willAnswer(REPLY_WITH_CORRECTION);

        long conversationId = conversationIdOf(send(null, "Yesterday I go to the supermarket."));

        assertThat(jdbc.sql("SELECT role FROM message WHERE conversation_id = :id ORDER BY id")
                .param("id", conversationId).query(String.class).list())
                .containsExactly("USER", "TUTOR");
        String trace = jdbc.sql("SELECT trace_json::text FROM message WHERE conversation_id = :id AND role = 'TUTOR'")
                .param("id", conversationId).query(String.class).single();
        assertThat(trace).contains("a1-past-simple").contains("\"fallback\": false");
    }

    @Test
    void sendsPreviousMessagesOfTheConversationAsHistory() {
        chatModel.willAnswer(REPLY_WITH_CORRECTION, REPLY_WITH_CORRECTION);
        long conversationId = conversationIdOf(send(null, "Yesterday I go to the supermarket."));

        send(conversationId, "I buy apples.");

        // system + (user, tutor) from the first turn + the new user message
        assertThat(chatModel.prompts().get(1).getInstructions()).hasSize(4);
    }

    @Test
    void answersWithFallbackWhenTheModelKeepsFailing() {
        chatModel.willAnswer("not json", "not json either");

        assertThat(send(null, "Hello!"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"fallback": true, "sources": []}
                        """);
    }

    @Test
    void unknownConversationReturns404() {
        assertThat(send(999_999L, "Hello!"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void blankTextReturns400() {
        assertThat(send(null, "  ")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(chatModel.prompts()).isEmpty();
    }

    private MvcTestResult send(Long conversationId, String text) {
        String body = conversationId == null
                ? "{\"text\": \"%s\"}".formatted(text)
                : "{\"conversationId\": %d, \"text\": \"%s\"}".formatted(conversationId, text);
        return mvc.post().uri("/api/chat").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private long conversationIdOf(MvcTestResult result) {
        try {
            Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.conversationId");
            return id.longValue();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
