package org.shark.renovatio.domain.model;

/** Runtime-neutral contract for baseline and target replay adapters. */
public interface ReplayRunner {
    ReplayResult run(ReplayInput input);

    record ReplayInput(String caseId, java.util.Map<String, ?> values) {
        public ReplayInput {
            if (caseId == null || caseId.isBlank()) throw new IllegalArgumentException("caseId is required");
            values = values == null ? java.util.Map.of() : java.util.Map.copyOf(values);
        }
    }
    record ReplayResult(String status, java.util.Map<String, ?> output,
                        java.util.List<String> stateChanges, java.util.List<String> externalCalls, String error) {
        public ReplayResult {
            status = status == null ? "UNKNOWN" : status;
            output = output == null ? java.util.Map.of() : java.util.Map.copyOf(output);
            stateChanges = stateChanges == null ? java.util.List.of() : java.util.List.copyOf(stateChanges);
            externalCalls = externalCalls == null ? java.util.List.of() : java.util.List.copyOf(externalCalls);
        }
    }
}
