package dev.lumi.curriculum;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
class CurriculumAdminController {

    private final CurriculumIngestionService ingestion;

    CurriculumAdminController(CurriculumIngestionService ingestion) {
        this.ingestion = ingestion;
    }

    /** Re-indexes lessons that changed since the last run. */
    @PostMapping("/ingest")
    CurriculumIngestionService.IngestionReport ingest() {
        return ingestion.ingestAll();
    }
}
