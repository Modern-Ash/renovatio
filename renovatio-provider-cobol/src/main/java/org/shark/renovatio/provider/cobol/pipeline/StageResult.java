package org.shark.renovatio.provider.cobol.pipeline;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Result of a single pipeline stage execution.
 */
public record StageResult(
    String stageName,
    boolean success,
    Instant startTime,
    Instant endTime,
    Duration duration,
    String output,
    List<String> errors,
    List<String> warnings
) {
    /**
     * Create a successful stage result.
     */
    public static StageResult success(String stageName, Instant startTime, Instant endTime, String output) {
        return new StageResult(
            stageName,
            true,
            startTime,
            endTime,
            Duration.between(startTime, endTime),
            output,
            List.of(),
            List.of()
        );
    }

    /**
     * Create a failed stage result.
     */
    public static StageResult failure(String stageName, Instant startTime, Instant endTime, List<String> errors) {
        return new StageResult(
            stageName,
            false,
            startTime,
            endTime,
            Duration.between(startTime, endTime),
            null,
            errors,
            List.of()
        );
    }

    /**
     * Create a stage result with warnings.
     */
    public static StageResult withWarnings(String stageName, Instant startTime, Instant endTime, 
                                          String output, List<String> warnings) {
        return new StageResult(
            stageName,
            true,
            startTime,
            endTime,
            Duration.between(startTime, endTime),
            output,
            List.of(),
            warnings
        );
    }
}
