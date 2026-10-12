package dev.lumi.tutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class TutorReplyTest {

    @Test
    void dropsSourcesThatWereNotRetrieved() {
        TutorReply reply = new TutorReply("Nice!", null, null, List.of("a1-past-simple", "invented-lesson"));

        assertThat(reply.sanitized(Set.of("a1-past-simple")).sources()).containsExactly("a1-past-simple");
    }

    @Test
    void dropsDuplicatedSources() {
        TutorReply reply = new TutorReply("Nice!", null, null, List.of("a1-to-be", "a1-to-be"));

        assertThat(reply.sanitized(Set.of("a1-to-be")).sources()).containsExactly("a1-to-be");
    }

    @Test
    void dropsCorrectionThatChangesNothing() {
        var noChange = new TutorReply.Correction("I am happy.", " i am happy. ");
        TutorReply reply = new TutorReply("Great!", null, noChange, List.of());

        assertThat(reply.sanitized(Set.of()).correction()).isNull();
    }

    @Test
    void turnsBlankExplanationIntoNull() {
        TutorReply reply = new TutorReply("Great!", "  ", null, List.of());

        assertThat(reply.sanitized(Set.of()).explanationPt()).isNull();
    }

    @Test
    void keepsRealCorrection() {
        var correction = new TutorReply.Correction("Yesterday I go.", "Yesterday I went.");
        TutorReply reply = new TutorReply("Nice!", "Go vira went.", correction, List.of());

        assertThat(reply.sanitized(Set.of()).correction()).isEqualTo(correction);
    }
}
