package dev.lumi.tutor;

import java.util.List;

/**
 * The tutor's turn. {@code sources} are the lessons the reply is based on, with titles for display;
 * {@code fallback} is true when the model failed and a default reply was sent instead.
 */
public record ChatResponse(
        long conversationId,
        long messageId,
        TutorReply reply,
        List<Source> sources,
        boolean fallback) {

    public record Source(String slug, String title) {
    }
}
