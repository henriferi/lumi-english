package dev.lumi.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private final LearnerSettingsRepository repository;

    SettingsService(LearnerSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SettingsResponse get() {
        return SettingsResponse.from(load());
    }

    @Transactional
    public SettingsResponse update(SettingsRequest request) {
        LearnerSettings settings = load();
        settings.update(request.name().strip(), request.ageBand(), request.level());
        return SettingsResponse.from(settings);
    }

    private LearnerSettings load() {
        return repository.findById(LearnerSettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("learner_settings row is missing; check the Flyway migrations"));
    }
}
