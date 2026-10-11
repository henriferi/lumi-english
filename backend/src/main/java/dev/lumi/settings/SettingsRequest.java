package dev.lumi.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SettingsRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull AgeBand ageBand,
        @NotNull Level level) {
}
