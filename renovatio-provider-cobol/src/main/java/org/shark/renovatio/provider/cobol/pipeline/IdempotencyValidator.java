package org.shark.renovatio.provider.cobol.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Validates repeated application and stale-input rejection from durable outputs. */
public class IdempotencyValidator {

    public IdempotencyReport validate(PipelineOrchestrator pipeline, PipelineRequest request) {
        PipelineResult first = pipeline.execute(request);
        Map<String, String> before = snapshot(first, "first-run");
        PipelineResult second = pipeline.execute(request);
        Map<String, String> after = snapshot(second, "second-run");
        List<Divergence> divergences = compareTrees(before, after);
        if (!first.isSuccessful() || !second.isSuccessful()) {
            divergences.add(new Divergence("pipeline", "two successful runs",
                first.success() + "/" + second.success(), Divergence.Type.CONTENT));
        }
        if (before.isEmpty()) {
            divergences.add(new Divergence("generated-tree", "at least one generated file", "empty",
                Divergence.Type.CONTENT));
        }
        return new IdempotencyReport(request.fixtureId(), divergences.isEmpty(),
            List.copyOf(divergences), first, second);
    }

    public IdempotencyReport validateStaleDetection(PipelineOrchestrator pipeline,
                                                     PipelineRequest request,
                                                     PipelineRequest modifiedRequest) {
        PipelineResult first = pipeline.execute(request);
        PipelineResult second = pipeline.execute(modifiedRequest);
        boolean staleDetected = !second.success() && second.semanticGaps().stream()
            .anyMatch(gap -> gap.isBlocking() && "STALE_SOURCE".equals(gap.statementType()));
        List<Divergence> divergences = staleDetected ? List.of() : List.of(new Divergence(
            "stale-detection", "blocking STALE_SOURCE action item", "not reported",
            Divergence.Type.CONTENT));
        return new IdempotencyReport(request.fixtureId(), staleDetected,
            divergences, first, second);
    }

    private Map<String, String> snapshot(PipelineResult result, String run) {
        try {
            return GeneratedTreeSnapshot.capture(result.outputDir());
        } catch (IOException exception) {
            return Map.of("!" + run + "-snapshot-error", exception.getClass().getName());
        }
    }

    private List<Divergence> compareTrees(Map<String, String> first, Map<String, String> second) {
        List<Divergence> divergences = new ArrayList<>();
        TreeSet<String> paths = new TreeSet<>(first.keySet());
        paths.addAll(second.keySet());
        for (String path : paths) {
            String before = first.get(path);
            String after = second.get(path);
            if (!java.util.Objects.equals(before, after)) {
                divergences.add(new Divergence(path, before == null ? "missing" : before,
                    after == null ? "missing" : after, Divergence.Type.CONTENT));
            }
        }
        return divergences;
    }

    public record IdempotencyReport(String fixtureId, boolean idempotent,
                                    List<Divergence> divergences,
                                    PipelineResult firstRun, PipelineResult secondRun) {
        public boolean isIdempotent() {
            return idempotent;
        }
    }

    public record Divergence(String location, String value1, String value2, Type type) {
        public enum Type { CONTENT, WHITESPACE, ORDERING, METADATA }
    }
}
