package dev.lumi.curriculum;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Indexes /curriculum/*.md into pgvector: parse → one chunk per section → embeddings → PgVectorStore.
 * A SHA-256 of each file is stored in curriculum_file, so unchanged lessons are not embedded again (saves tokens).
 */
@Service
public class CurriculumIngestionService {

    private static final Logger log = LoggerFactory.getLogger(CurriculumIngestionService.class);

    private final CurriculumProperties properties;
    private final LessonParser parser;
    private final VectorStore vectorStore;
    private final JdbcClient jdbc;
    private final TransactionTemplate transaction;

    CurriculumIngestionService(CurriculumProperties properties, LessonParser parser, VectorStore vectorStore,
            JdbcClient jdbc, TransactionTemplate transaction) {
        this.properties = properties;
        this.parser = parser;
        this.vectorStore = vectorStore;
        this.jdbc = jdbc;
        this.transaction = transaction;
    }

    /** Synchronized: startup ingestion and the admin endpoint must not index the same file at the same time. */
    public synchronized IngestionReport ingestAll() {
        List<Path> files = listLessonFiles();
        Map<String, String> knownHashes = jdbc.sql("SELECT path, content_hash FROM curriculum_file")
                .query((rs, row) -> Map.entry(rs.getString("path"), rs.getString("content_hash")))
                .list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<String> indexed = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (Path file : files) {
            String source = file.getFileName().toString();
            String content = read(file);
            String hash = sha256(content);
            if (hash.equals(knownHashes.get(source))) {
                skipped.add(source);
                continue;
            }
            try {
                Lesson lesson = parser.parse(content);
                transaction.executeWithoutResult(status -> index(source, hash, lesson));
                indexed.add(source);
            } catch (InvalidLessonException e) {
                log.error("Skipping invalid lesson {}: {}", source, e.getMessage());
                failed.add(source);
            }
        }

        List<String> currentSources = files.stream().map(f -> f.getFileName().toString()).toList();
        List<String> removed = knownHashes.keySet().stream()
                .filter(source -> !currentSources.contains(source))
                .sorted()
                .toList();
        removed.forEach(source -> transaction.executeWithoutResult(status -> remove(source)));

        IngestionReport report = new IngestionReport(indexed, skipped, removed, failed);
        log.info("Curriculum ingestion: {} indexed, {} unchanged, {} removed, {} failed",
                indexed.size(), skipped.size(), removed.size(), failed.size());
        return report;
    }

    private void index(String source, String hash, Lesson lesson) {
        deleteChunks(source); // a changed lesson replaces all of its old chunks
        vectorStore.add(LessonChunks.toDocuments(lesson, source));
        jdbc.sql("""
                INSERT INTO skill (tag, name_pt, level) VALUES (:tag, :namePt, :level)
                ON CONFLICT (tag) DO UPDATE SET name_pt = EXCLUDED.name_pt, level = EXCLUDED.level
                """)
                .param("tag", lesson.skill())
                .param("namePt", lesson.titlePt())
                .param("level", lesson.level().name())
                .update();
        jdbc.sql("""
                INSERT INTO curriculum_file (path, content_hash, ingested_at) VALUES (:path, :hash, now())
                ON CONFLICT (path) DO UPDATE SET content_hash = EXCLUDED.content_hash, ingested_at = now()
                """)
                .param("path", source)
                .param("hash", hash)
                .update();
    }

    private void remove(String source) {
        deleteChunks(source);
        jdbc.sql("DELETE FROM curriculum_file WHERE path = :path").param("path", source).update();
    }

    private void deleteChunks(String source) {
        vectorStore.delete("%s == '%s'".formatted(LessonChunks.SOURCE, source));
    }

    private List<Path> listLessonFiles() {
        Path dir = Path.of(properties.path());
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException("Curriculum folder not found: " + dir.toAbsolutePath().normalize());
        }
        try (Stream<Path> paths = Files.list(dir)) {
            return paths.filter(p -> p.getFileName().toString().endsWith(".md")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sha256(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record IngestionReport(List<String> indexed, List<String> unchanged, List<String> removed,
            List<String> failed) {
    }
}
