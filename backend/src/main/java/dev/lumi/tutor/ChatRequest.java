package dev.lumi.tutor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A learner message. Without a conversationId a new conversation is started. */
public record ChatRequest(
        Long conversationId,
        @NotBlank @Size(max = 500) String text) {
}
