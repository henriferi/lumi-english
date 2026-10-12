package dev.lumi.tutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import dev.lumi.curriculum.RetrievedChunk;
import dev.lumi.settings.AgeBand;
import dev.lumi.settings.Level;
import dev.lumi.settings.SettingsResponse;
import org.junit.jupiter.api.Test;

class TutorPromptBuilderTest {

    private final TutorPromptBuilder builder = new TutorPromptBuilder();

    private static final RetrievedChunk PAST_SIMPLE = new RetrievedChunk("a1-past-simple", "Past simple", "Rule",
            "Past simple — Rule\n\ngo → went", 0.8);

    @Test
    void includesTheLearnerProfile() {
        String prompt = builder.systemPrompt(new SettingsResponse("Ana", AgeBand.ADULT, Level.A1), List.of());

        assertThat(prompt).contains("The learner's name is Ana").contains("English level is A1");
    }

    @Test
    void personaDependsOnTheAgeGroup() {
        assertThat(promptFor(AgeBand.KID, Level.A1)).contains("The learner is a child").contains("animals");
        assertThat(promptFor(AgeBand.TEEN, Level.A1)).contains("The learner is a teenager").contains("music");
        assertThat(promptFor(AgeBand.ADULT, Level.A1)).contains("The learner is an adult").contains("work");
    }

    @Test
    void useOfPortugueseDependsOnTheLevel() {
        assertThat(promptFor(AgeBand.ADULT, Level.PRE_A1)).contains("always explain in Portuguese");
        assertThat(promptFor(AgeBand.ADULT, Level.A1)).contains("Use Portuguese only in \"explanationPt\"");
        assertThat(promptFor(AgeBand.ADULT, Level.A2)).contains("Write everything in English");
    }

    @Test
    void pastesRetrievedSectionsWithTheirSlug() {
        String prompt = builder.systemPrompt(new SettingsResponse("Ana", AgeBand.ADULT, Level.A1), List.of(PAST_SIMPLE));

        assertThat(prompt).contains("[slug: a1-past-simple]").contains("go → went");
        assertThat(prompt).doesNotContain(TutorPromptBuilder.NO_CONTEXT);
    }

    @Test
    void tellsTheModelNotToInventRulesWhenNothingWasRetrieved() {
        String prompt = builder.systemPrompt(new SettingsResponse("Ana", AgeBand.ADULT, Level.A1), List.of());

        assertThat(prompt).contains(TutorPromptBuilder.NO_CONTEXT);
    }

    @Test
    void learnerNameWithBracesDoesNotBreakTheTemplate() {
        String prompt = builder.systemPrompt(new SettingsResponse("{Ana}", AgeBand.ADULT, Level.A1), List.of());

        assertThat(prompt).contains("The learner's name is {Ana}");
    }

    private String promptFor(AgeBand ageBand, Level level) {
        return builder.systemPrompt(new SettingsResponse("Ana", ageBand, level), List.of());
    }
}
