package dev.lumi.settings;

import org.springframework.data.jpa.repository.JpaRepository;

interface LearnerSettingsRepository extends JpaRepository<LearnerSettings, Short> {
}
