package dev.lumi.tutor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Bound from "lumi.tutor.*" in application.yml. */
@Validated
@ConfigurationProperties("lumi.tutor")
public record TutorProperties(@Min(0) @Max(50) int historySize) {
}
