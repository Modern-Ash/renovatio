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
    StageResult domainModel,
    StageResult decisions,
    StageResult architecture,
    StageResult manifest,
    StageResult emit,
    StageResult build,
    EquivalenceReport equivalence,
    List<EquivalenceReport> equivalenceReports,
    List<ActionItem> semanticGaps,
    boolean success
) {
    public PipelineResult {
        equivalenceReports = equivalenceReports == null ? List.of() : List.copyOf(equivalenceReports);
        semanticGaps = semanticGaps == null ? List.of() : List.copyOf(semanticGaps);
    }

    /**
     * Check if the pipeline completed successfully.
     */
    public boolean isSuccessful() {
        return success
               && stageSucceeded(discover)
               && stageSucceeded(parse)
               && stageSucceeded(semanticIr)
               && stageSucceeded(domainModel)
               && stageSucceeded(decisions)
               && stageSucceeded(architecture)
               && stageSucceeded(manifest)
               && stageSucceeded(emit)
               && stageSucceeded(build)
               && !hasBlockingGaps()
               && equivalenceReports.stream().allMatch(EquivalenceReport::isPassed)
               && (equivalence == null || equivalence.isPassed());
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

    private static boolean stageSucceeded(StageResult stage) {
        return stage != null && stage.success();
    }
}
