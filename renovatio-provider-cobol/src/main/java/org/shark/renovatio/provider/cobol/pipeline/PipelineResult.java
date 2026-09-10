package org.shark.renovatio.provider.cobol.pipeline;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Complete result of pipeline execution for a fixture.
 */
public record PipelineResult(
    String fixtureId,
    Path outputDir,
    Instant startTime,
    Instant endTime,
    Duration totalDuration,
    StageResult discover,
    StageResult parse,
    StageResult semanticIr,
    StageResult decisions,
    StageResult manifest,
    StageResult emit,
    StageResult openRewrite,
    StageResult build,
    EquivalenceReport equivalence,
    List<ActionItem> semanticGaps,
    boolean success
) {
    /**
     * Check if the pipeline completed successfully.
     */
    public boolean isSuccessful() {
        return success && 
               discover.success() && 
               parse.success() && 
               semanticIr.success() && 
               decisions.success() && 
               manifest.success() && 
               emit.success() && 
               openRewrite.success() && 
               build.success() &&
               (equivalence == null || equivalence.isPassed());
    }

    /**
     * Check if there are any blocking semantic gaps.
     */
    public boolean hasBlockingGaps() {
        return semanticGaps.stream()
            .anyMatch(ActionItem::isBlocking);
    }

    /**
     * Get count of semantic gaps by severity.
     */
    public long countGaps(ActionItem.Severity severity) {
        return semanticGaps.stream()
            .filter(gap -> gap.severity() == severity)
            .count();
    }
}
