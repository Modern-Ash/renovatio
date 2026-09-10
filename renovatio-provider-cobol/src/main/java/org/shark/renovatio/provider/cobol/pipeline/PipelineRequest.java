package org.shark.renovatio.provider.cobol.pipeline;

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
    boolean verifyEquivalence
) {
    /**
     * Create a request with default options.
     */
    public static PipelineRequest of(Path fixtureDir, Path outputDir) {
        return new PipelineRequest(
            fixtureDir.getFileName().toString(),
            fixtureDir,
            Map.of(),
            outputDir,
            fixtureDir.resolve("expected"),
            false,
            true
        );
    }

    /**
     * Create a request with determinism verification.
     */
    public static PipelineRequest withDeterminism(Path fixtureDir, Path outputDir) {
        return new PipelineRequest(
            fixtureDir.getFileName().toString(),
            fixtureDir,
            Map.of(),
            outputDir,
            fixtureDir.resolve("expected"),
            true,
            true
        );
    }
}
