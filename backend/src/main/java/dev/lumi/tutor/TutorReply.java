package dev.lumi.tutor;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The tutor's answer as structured JSON. Its JSON schema is generated from this record and sent to OpenAI
 * (structured outputs); the descriptions below become part of that schema and guide the model.
 */
public record TutorReply(
        @JsonPropertyDescription("Your answer to the learner, in English, ending with a simple question.")
        @NotBlank @Size(max = 2000) String reply,

        @JsonPropertyDescription("Short explanation in Brazilian Portuguese of the correction or rule, or null.")
        @Size(max = 1000) String explanationPt,

        @JsonPropertyDescription("The most important mistake in the learner's last message, or null if it was correct.")
        @Valid Correction correction,

        @JsonPropertyDescription("Slugs of the lessons you used, only from the lessons given to you. Empty if none.")
        @NotNull List<String> sources) {

    static final TutorReply FALLBACK = new TutorReply(
            "Sorry, I had a problem answering. Can you send your message again?",
            "Tive um problema para responder agora. Tente enviar a mensagem de novo.",
            null,
            List.of());

    public record Correction(
            @JsonPropertyDescription("The learner's sentence exactly as written.")
            @NotBlank @Size(max = 500) String original,

            @JsonPropertyDescription("The same sentence, corrected.")
            @NotBlank @Size(max = 500) String corrected) {
    }

    /**
     * Applies rules the model can't be trusted with: a source must be a lesson we actually retrieved, a
     * "correction" that changes nothing is not a correction, and an empty explanation is no explanation.
     */
    TutorReply sanitized(Set<String> retrievedSlugs) {
        Correction cleanCorrection = correction != null && !sameSentence(correction.original(), correction.corrected())
                ? correction
                : null;
        String cleanExplanation = explanationPt == null || explanationPt.isBlank() ? null : explanationPt.strip();
        List<String> cleanSources = sources.stream().filter(retrievedSlugs::contains).distinct().toList();
        return new TutorReply(reply, cleanExplanation, cleanCorrection, cleanSources);
    }

    private static boolean sameSentence(String a, String b) {
        return a.strip().equalsIgnoreCase(b.strip());
    }
}
