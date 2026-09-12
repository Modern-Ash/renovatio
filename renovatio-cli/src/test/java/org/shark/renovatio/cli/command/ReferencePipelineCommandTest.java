package org.shark.renovatio.cli.command;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.provider.cobol.pipeline.PipelineResult;
import org.shark.renovatio.provider.cobol.pipeline.StageResult;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReferencePipelineCommandTest {

    @Test
    void rendersFailedPartialPipelineWithoutNullStageFailure() {
        Instant start = Instant.parse("2026-09-11T00:00:00Z");
        Instant end = start.plusMillis(25);
        PipelineResult result = new PipelineResult(
                "empty-workspace",
                Path.of("/tmp/reference-output"),
                start,
                end,
                Duration.between(start, end),
                StageResult.failure("discover", start, end, List.of("no COBOL sources found")),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                false);

        var view = ReferencePipelineCommand.view(result);

        assertThat(view).containsEntry("success", false);
        assertThat(view).containsEntry("stageCount", 1);
        assertThat(view).containsEntry("passedStages", 0L);
        assertThat(view.get("stages")).asList().hasSize(1);
        assertThat(view).containsEntry("message", "reference pipeline failed");
    }
}
