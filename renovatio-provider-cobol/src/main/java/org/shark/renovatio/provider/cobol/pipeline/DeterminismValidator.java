package org.shark.renovatio.provider.cobol.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Validates determinism from independently generated file trees. */
public class DeterminismValidator {

    public DeterminismReport validate(PipelineOrchestrator pipeline, PipelineRequest request, int runs) {
        if (runs < 2) {
            throw new IllegalArgumentException("Determinism validation requires at least two runs");
        }
        List<PipelineResult> results = new ArrayList<>();
        List<Map<String, String>> snapshots = new ArrayList<>();
        List<Divergence> divergences = new ArrayList<>();

        for (int index = 0; index < runs; index++) {
            try {
                Path output = independentOutput(request.outputDir(), index);
                PipelineResult result = pipeline.execute(request.withOutputDir(output));
                results.add(result);
                if (!result.isSuccessful()) {
                    divergences.add(new Divergence("run-" + index, "successful", "failed",
                        Divergence.Type.CONTENT));
                }
                snapshots.add(GeneratedTreeSnapshot.capture(result.outputDir()));
            } catch (IOException exception) {
                divergences.add(new Divergence("run-" + index, "readable generated tree",
                    exception.getMessage(), Divergence.Type.CONTENT));
                snapshots.add(Map.of());
            }
        }

        for (int index = 1; index < snapshots.size(); index++) {
            divergences.addAll(compareTrees(snapshots.get(0), snapshots.get(index), index));
        }
        if (snapshots.get(0).isEmpty()) {
            divergences.add(new Divergence("generated-tree", "at least one generated file", "empty",
                Divergence.Type.CONTENT));
        }
        return new DeterminismReport(request.fixtureId(), runs, divergences.isEmpty(),
            List.copyOf(divergences), results.get(0));
    }

    private Path independentOutput(Path requested, int index) throws IOException {
        Path absolute = requested.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent == null) {
            parent = Path.of(".").toAbsolutePath().normalize();
        }
        Files.createDirectories(parent);
        return Files.createTempDirectory(parent, absolute.getFileName() + "-run-" + index + "-");
    }

    private List<Divergence> compareTrees(Map<String, String> baseline, Map<String, String> candidate,
                                          int run) {
        List<Divergence> result = new ArrayList<>();
        TreeSet<String> paths = new TreeSet<>(baseline.keySet());
        paths.addAll(candidate.keySet());
        for (String path : paths) {
            String first = baseline.get(path);
            String next = candidate.get(path);
            if (first == null) {
                result.add(new Divergence(path, "missing", next, Divergence.Type.CONTENT));
            } else if (next == null) {
                result.add(new Divergence(path, first, "missing in run " + run,
                    Divergence.Type.CONTENT));
            } else if (!first.equals(next)) {
                result.add(new Divergence(path, first, next, Divergence.Type.CONTENT));
            }
        }
        return result;
    }

    public record DeterminismReport(String fixtureId, int runs, boolean deterministic,
                                    List<Divergence> divergences, PipelineResult sampleResult) {
        public boolean isDeterministic() {
            return deterministic;
        }
    }

    public record Divergence(String location, String value1, String value2, Type type) {
        public enum Type { CONTENT, WHITESPACE, ORDERING, METADATA }
    }
}
