package dev.lumi.settings;

public record SettingsResponse(String name, AgeBand ageBand, Level level) {

    static SettingsResponse from(LearnerSettings settings) {
        return new SettingsResponse(settings.getName(), settings.getAgeBand(), settings.getLevel());
    }
}
