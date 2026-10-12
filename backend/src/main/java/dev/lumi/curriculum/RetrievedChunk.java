package dev.lumi.curriculum;

/** A lesson section returned by the vector search, with its similarity score (0..1, higher is closer). */
public record RetrievedChunk(String slug, String title, String section, String content, double score) {
}
