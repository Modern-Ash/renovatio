package org.shark.renovatio.cli.command;

import org.shark.renovatio.provider.cobol.pipeline.ActionItem;
import org.shark.renovatio.provider.cobol.pipeline.EquivalenceReport;
import org.shark.renovatio.provider.cobol.pipeline.PipelineRequest;
import org.shark.renovatio.provider.cobol.pipeline.PipelineResult;
import org.shark.renovatio.provider.cobol.pipeline.StageResult;
import org.shark.renovatio.provider.cobol.service.CobolReferencePipelineService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Command(name = "reference-pipeline", mixinStandardHelpOptions = true,
        description = "Execute the governed COBOL-to-Java reference pipeline.")
public class ReferencePipelineCommand extends AbstractCoreCommand {

    @Parameters(index = "0", paramLabel = "<path>",
            description = "Reference fixture or project workspace containing COBOL sources.")
    Path path;

    @Option(names = "--out", required = true, description = "Directory for generated Java output.")
    Path out;

    @Option(names = "--expected", description = "Expected-output directory for equivalence.")
    Path expected;

    @Option(names = "--no-equivalence", description = "Skip golden-tree equivalence checks.")
    boolean noEquivalence;

    @Override
    public Integer call() {
        Path workspace = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(workspace)) {
            return output().render(Map.of("success", false,
                    "message", "workspace directory not found: " + workspace), ignored -> { });
        }

        Path outputDir = resolveAgainstWorkspace(workspace, out);
        Path expectedDir = expected == null ? workspace.resolve("expected") : resolveAgainstWorkspace(workspace, expected);
        PipelineRequest base = PipelineRequest.of(workspace, outputDir);
        PipelineRequest request = new PipelineRequest(base.fixtureId(), base.fixtureDir(), base.decisions(),
                base.outputDir(), expectedDir, base.deterministic(), !noEquivalence, base.sourceSnapshotHash());

        PipelineResult result = context().bean(CobolReferencePipelineService.class).execute(request);
        Map<String, Object> view = view(result);
        return output().render(view, rendered -> {
            output().line("fixture: " + rendered.get("fixtureId"));
            output().line("output: " + rendered.get("outputPath"));
            output().line("stages: " + rendered.get("passedStages") + "/" + rendered.get("stageCount"));
            output().line("semantic gaps: " + rendered.get("semanticGapCount")
                    + " (blocking: " + rendered.get("blockingGapCount") + ")");
            output().line("equivalence: " + rendered.get("equivalencePassed"));
        });
    }

    private static Path resolveAgainstWorkspace(Path workspace, Path value) {
        return value.isAbsolute() ? value.normalize() : workspace.resolve(value).normalize();
    }

    static Map<String, Object> view(PipelineResult result) {
        Map<String, Object> view = new LinkedHashMap<>();
        List<StageResult> stages = Stream.of(result.discover(), result.parse(), result.semanticIr(),
                result.domainModel(), result.decisions(), result.architecture(), result.manifest(),
                result.emit(), result.build())
                .filter(Objects::nonNull)
                .toList();
        long passed = stages.stream().filter(StageResult::success).count();
        long blocking = result.semanticGaps().stream().filter(ActionItem::isBlocking).count();
        boolean equivalencePassed = result.equivalenceReports().stream().allMatch(EquivalenceReport::isPassed)
                && (result.equivalence() == null || result.equivalence().isPassed());

        view.put("success", result.isSuccessful());
        view.put("fixtureId", result.fixtureId());
        view.put("outputPath", result.outputDir().toString());
        view.put("durationMs", result.totalDuration().toMillis());
        view.put("stageCount", stages.size());
        view.put("passedStages", passed);
        view.put("semanticGapCount", result.semanticGaps().size());
        view.put("blockingGapCount", blocking);
        view.put("equivalencePassed", equivalencePassed);
        view.put("semanticGaps", result.semanticGaps());
        view.put("equivalenceReports", result.equivalenceReports());
        view.put("stages", stages);
        if (!result.isSuccessful()) {
            view.put("message", "reference pipeline failed");
        }
        return view;
    }
}
