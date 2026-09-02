package org.shark.renovatio.jcl.characterization;

import org.shark.renovatio.semantic.ir.BatchDataset;
import org.shark.renovatio.semantic.ir.BatchJob;
import org.shark.renovatio.semantic.ir.BatchStep;
import org.shark.renovatio.semantic.ir.ConditionGraph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-process hook for comparing a projected batch job with reference fixture outputs. */
public final class BatchCharacterizationHarness {
    public RunResult run(BatchJob job, Map<String, List<String>> inputs, StepExecutor executor) throws Exception {
        LinkedHashMap<String, List<String>> data = new LinkedHashMap<>();
        if (inputs != null) inputs.forEach((key, value) -> data.put(key, new ArrayList<>(value)));
        LinkedHashMap<String, Integer> returnCodes = new LinkedHashMap<>();
        List<String> executed = new ArrayList<>();
        for (BatchStep step : job.steps()) {
            if (!shouldRun(job, step, returnCodes)) continue;
            int returnCode = executor.execute(step, data);
            returnCodes.put(step.stepName(), returnCode);
            executed.add(step.stepName());
        }
        job.datasets().stream().filter(dataset -> dataset.access() == BatchDataset.AccessKind.TEMP)
                .forEach(dataset -> {
                    data.remove(dataset.ddName());
                    dataset.resourceReference().ifPresent(data::remove);
                });
        LinkedHashMap<String, List<String>> immutable = new LinkedHashMap<>();
        data.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
        return new RunResult(java.util.Collections.unmodifiableMap(immutable),
                java.util.Collections.unmodifiableMap(returnCodes), List.copyOf(executed));
    }

    private static boolean shouldRun(BatchJob job, BatchStep step, Map<String, Integer> returnCodes) {
        for (ConditionGraph.Guard guard : job.conditionGraph().guards()) {
            if (!guard.memberStepIds().contains(step.id())) continue;
            if (guard.referencedStepId().isEmpty()) continue;
            BatchStep referenced = job.steps().stream()
                    .filter(value -> value.id().equals(guard.referencedStepId().get())).findFirst().orElseThrow();
            Integer rc = returnCodes.get(referenced.stepName());
            if (rc == null) return false;
            Boolean run = guard.truthTable().get(referenced.stepName() + ".RC=" + rc);
            if (run == null) run = guard.truthTable().get("ANY.RC=" + rc);
            if (run != null && !run) return false;
        }
        return true;
    }

    @FunctionalInterface
    public interface StepExecutor {
        int execute(BatchStep step, Map<String, List<String>> datasets) throws Exception;
    }

    public record RunResult(Map<String, List<String>> datasets, Map<String, Integer> returnCodes,
                            List<String> executedSteps) { }
}
