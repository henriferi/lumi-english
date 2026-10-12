package dev.lumi.curriculum;

import java.util.List;

import dev.lumi.settings.Level;

/** A curriculum lesson parsed from /curriculum/*.md: front matter plus one section per "## " heading. */
public record Lesson(
        String slug,
        String title,
        String titlePt,
        Level level,
        String skill,
        List<Section> sections) {

    public record Section(String name, String content) {
    }
}
