package org.shark.renovatio.provider.cobol.pipeline;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Request for executing the COBOL-to-Java pipeline.
 * Contains all necessary inputs for pipeline execution.
 */
public record PipelineRequest(
    String fixtureId,
    Path fixtureDir,
    Map<String, Object> decisions,
    Path outputDir,
    Path expectedDir,
    boolean deterministic,
    boolean verifyEquivalence,
    String sourceSnapshotHash
) {
    public PipelineRequest {
        decisions = decisions == null ? Map.of() : Map.copyOf(decisions);
        sourceSnapshotHash = sourceSnapshotHash == null || sourceSnapshotHash.isBlank()
            ? SourceSnapshot.capture(fixtureDir) : sourceSnapshotHash;
    }

    /** Compatibility constructor that binds the request to the current source snapshot. */
    public PipelineRequest(String fixtureId, Path fixtureDir, Map<String, Object> decisions,
                           Path outputDir, Path expectedDir, boolean deterministic,
                           boolean verifyEquivalence) {
        this(fixtureId, fixtureDir, decisions, outputDir, expectedDir, deterministic,
            verifyEquivalence, SourceSnapshot.capture(fixtureDir));
    }

    /**
     * Create a request with default options.
     */
    public static PipelineRequest of(Path fixtureDir, Path outputDir) {
        return new PipelineRequest(
            fixtureDir.getFileName().toString(),
            fixtureDir,
            fixtureDecisions(fixtureDir),
            outputDir,
            fixtureDir.resolve("expected"),
            false,
            true,
            SourceSnapshot.capture(fixtureDir)
        );
    }

    /**
     * Create a request with determinism verification.
     */
    public static PipelineRequest withDeterminism(Path fixtureDir, Path outputDir) {
        return new PipelineRequest(
            fixtureDir.getFileName().toString(),
            fixtureDir,
            fixtureDecisions(fixtureDir),
            outputDir,
            fixtureDir.resolve("expected"),
            true,
            true,
            SourceSnapshot.capture(fixtureDir)
        );
    }

    public PipelineRequest withOutputDir(Path newOutputDir) {
        return new PipelineRequest(fixtureId, fixtureDir, decisions, newOutputDir, expectedDir,
            deterministic, verifyEquivalence, sourceSnapshotHash);
    }

    public PipelineRequest withFixtureDir(Path newFixtureDir) {
        return new PipelineRequest(fixtureId, newFixtureDir, decisions, outputDir, expectedDir,
            deterministic, verifyEquivalence, sourceSnapshotHash);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> fixtureDecisions(Path fixtureDir) {
        Path file = fixtureDir.resolve("decisions.json");
        if (!Files.isRegularFile(file)) return Map.of();
        try {
            Map<String, Object> document = new ObjectMapper().readValue(file.toFile(),
                new TypeReference<>() { });
            Object decisions = document.get("decisions");
            return decisions instanceof Map<?, ?> values
                ? (Map<String, Object>) values : Map.of();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read fixture decisions from " + file, exception);
        }
    }
}
