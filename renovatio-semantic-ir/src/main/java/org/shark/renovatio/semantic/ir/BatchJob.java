package org.shark.renovatio.semantic.ir;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Immutable, deterministic, target-neutral batch orchestration model. */
public record BatchJob(String schemaVersion, String jobId, SourceProvenance sourceProvenance,
                       List<BatchStep> steps, List<BatchDataset> datasets,
                       ConditionGraph conditionGraph) {
    public static final String SCHEMA_VERSION = "1";

    public BatchJob {
        if (!SCHEMA_VERSION.equals(schemaVersion)) throw new IllegalArgumentException("unsupported schemaVersion");
        jobId = SemanticIdentity.normalizeProgramId(jobId);
        sourceProvenance = Objects.requireNonNull(sourceProvenance, "sourceProvenance");
        steps = (steps == null ? List.<BatchStep>of() : steps).stream()
                .sorted(Comparator.comparingInt(BatchStep::ordinal).thenComparing(BatchStep::id)).toList();
        datasets = (datasets == null ? List.<BatchDataset>of() : datasets).stream()
                .sorted(Comparator.comparing(BatchDataset::ddName).thenComparing(BatchDataset::id)).toList();
        conditionGraph = conditionGraph == null ? new ConditionGraph(List.of()) : conditionGraph;

        requireUnique(steps.stream().map(BatchStep::id).toList(), "step id");
        requireUnique(steps.stream().map(BatchStep::stepName).toList(), "step name");
        requireUnique(datasets.stream().map(BatchDataset::id).toList(), "dataset id");
        List<BatchStep> orderedSteps = steps;
        Set<String> stepIds = new HashSet<>(steps.stream().map(BatchStep::id).toList());
        Set<String> datasetIds = new HashSet<>(datasets.stream().map(BatchDataset::id).toList());
        steps.forEach(step -> requireReferences(step.datasetRefs(), datasetIds, "dataset"));
        conditionGraph.guards().forEach(guard -> {
            requireReferences(guard.memberStepIds(), stepIds, "guard member step");
            guard.referencedStepId().ifPresent(id -> requireReferences(List.of(id), stepIds, "guard referenced step"));
            if (guard.referencedStepId().isPresent()) {
                int referencedOrdinal = orderedSteps.stream().filter(step -> step.id().equals(guard.referencedStepId().get()))
                        .findFirst().orElseThrow().ordinal();
                guard.memberStepIds().forEach(member -> {
                    int memberOrdinal = orderedSteps.stream().filter(step -> step.id().equals(member))
                            .findFirst().orElseThrow().ordinal();
                    if (referencedOrdinal >= memberOrdinal)
                        throw new IllegalArgumentException("guard must reference a prior step");
                });
            }
        });
    }

    public static String stepId(String jobId, String stepName, int ordinal) {
        return stableId("step", jobId, stepName, Integer.toString(ordinal));
    }

    public static String datasetId(String jobId, String stepName, String ddName) {
        return stableId("dataset", jobId, stepName, ddName);
    }

    private static String stableId(String kind, String... parts) {
        StringBuilder value = new StringBuilder("batch-ir.v1\n").append(kind);
        for (String part : parts) value.append('\n').append(Objects.requireNonNull(part).trim().toUpperCase(Locale.ROOT));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static void requireUnique(List<String> values, String label) {
        if (new HashSet<>(values).size() != values.size()) throw new IllegalArgumentException("duplicate " + label);
    }

    private static void requireReferences(List<String> values, Set<String> available, String label) {
        values.forEach(value -> { if (!available.contains(value)) throw new IllegalArgumentException("dangling " + label); });
    }
}
