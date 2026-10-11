package dev.lumi.settings;

import static org.assertj.core.api.Assertions.assertThat;

import dev.lumi.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SettingsIntegrationTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    SettingsService service;

    @Autowired
    LearnerSettingsRepository repository;

    @BeforeEach
    void resetToMigrationDefaults() {
        service.update(new SettingsRequest("Learner", AgeBand.ADULT, Level.PRE_A1));
    }

    @Test
    void getReturnsTheSingleSettingsRow() {
        assertThat(mvc.get().uri("/api/settings"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"name": "Learner", "ageBand": "ADULT", "level": "PRE_A1"}
                        """);
    }

    @Test
    void putUpdatesAndPersistsTheSettings() {
        assertThat(mvc.put().uri("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "  Ana  ", "ageBand": "KID", "level": "A1"}
                        """))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"name": "Ana", "ageBand": "KID", "level": "A1"}
                        """);

        assertThat(repository.findAll()).singleElement().satisfies(saved -> {
            assertThat(saved.getName()).isEqualTo("Ana");
            assertThat(saved.getAgeBand()).isEqualTo(AgeBand.KID);
            assertThat(saved.getLevel()).isEqualTo(Level.A1);
        });
    }

    @Test
    void putWithInvalidFieldsReturnsProblemDetails() {
        assertThat(mvc.put().uri("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": " ", "ageBand": null, "level": "A1"}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .hasPathSatisfying("$.title", title -> assertThat(title).isEqualTo("Validation failed"))
                .hasPathSatisfying("$.errors.name", error -> assertThat(error).isNotNull())
                .hasPathSatisfying("$.errors.ageBand", error -> assertThat(error).isNotNull());

        assertThat(service.get().name()).isEqualTo("Learner");
    }

    @Test
    void putWithUnknownEnumValueReturnsBadRequest() {
        assertThat(mvc.put().uri("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Ana", "ageBand": "BABY", "level": "A1"}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
