package dev.lumi.curriculum;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * RAG settings, bound from "lumi.curriculum.*" in application.yml. {@code path} is the lessons folder relative to
 * the working directory; it is a String because Spring would resolve a Path property as a classpath resource.
 */
@Validated
@ConfigurationProperties("lumi.curriculum")
public record CurriculumProperties(
        @NotBlank String path,
        boolean ingestOnStartup,
        @Min(1) @Max(20) int topK,
        @DecimalMin("0.0") @DecimalMax("1.0") double similarityThreshold) {
}
