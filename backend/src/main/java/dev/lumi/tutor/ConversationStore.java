package dev.lumi.tutor;

import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Conversations and messages, with plain SQL (JdbcClient) because of the jsonb columns. */
@Repository
class ConversationStore {

    enum Role { USER, TUTOR }

    private final JdbcClient jdbc;
    private final JsonMapper json;

    ConversationStore(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    long createConversation() {
        return jdbc.sql("INSERT INTO conversation DEFAULT VALUES RETURNING id").query(Long.class).single();
    }

    boolean exists(long conversationId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM conversation WHERE id = :id)")
                .param("id", conversationId)
                .query(Boolean.class)
                .single();
    }

    /** The last {@code limit} messages, oldest first, as chat messages for the model. */
    List<Message> recentMessages(long conversationId, int limit) {
        List<Message> newestFirst = jdbc.sql("""
                SELECT role, text FROM message
                WHERE conversation_id = :conversationId
                ORDER BY created_at DESC, id DESC
                LIMIT :limit
                """)
                .param("conversationId", conversationId)
                .param("limit", limit)
                .query((rs, row) -> toMessage(Role.valueOf(rs.getString("role")), rs.getString("text")))
                .list();
        return newestFirst.reversed();
    }

    /** Saves the learner's message and the tutor's reply together; returns the tutor message id. */
    @Transactional
    long saveExchange(long conversationId, String learnerText, TutorReply reply, ChatTrace trace) {
        insert(conversationId, Role.USER, learnerText, null, null);
        return insert(conversationId, Role.TUTOR, reply.reply(), json.writeValueAsString(reply),
                json.writeValueAsString(trace));
    }

    private long insert(long conversationId, Role role, String text, String replyJson, String traceJson) {
        return jdbc.sql("""
                INSERT INTO message (conversation_id, role, text, reply_json, trace_json)
                VALUES (:conversationId, :role, :text, CAST(:replyJson AS jsonb), CAST(:traceJson AS jsonb))
                RETURNING id
                """)
                .param("conversationId", conversationId)
                .param("role", role.name())
                .param("text", text)
                .param("replyJson", replyJson)
                .param("traceJson", traceJson)
                .query(Long.class)
                .single();
    }

    private static Message toMessage(Role role, String text) {
        return role == Role.USER ? new UserMessage(text) : new AssistantMessage(text);
    }
}
