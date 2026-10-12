package dev.lumi.curriculum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Indexes the curriculum once the app is up. A failure (e.g. no OpenAI key) is logged, not fatal. */
@Component
class CurriculumStartupIngestion {

    private static final Logger log = LoggerFactory.getLogger(CurriculumStartupIngestion.class);

    private final CurriculumProperties properties;
    private final CurriculumIngestionService ingestion;

    CurriculumStartupIngestion(CurriculumProperties properties, CurriculumIngestionService ingestion) {
        this.properties = properties;
        this.ingestion = ingestion;
    }

    @EventListener(ApplicationReadyEvent.class)
    void ingestOnStartup() {
        if (!properties.ingestOnStartup()) {
            return;
        }
        try {
            ingestion.ingestAll();
        } catch (RuntimeException e) {
            log.error("Curriculum ingestion failed; the tutor will answer without lessons. "
                    + "Fix the cause and call POST /api/admin/ingest.", e);
        }
    }
}
