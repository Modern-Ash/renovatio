package org.shark.renovatio.cobol.ir.annotated;

import java.util.List;

public record ControlFlowPlanPayload(List<String> affectedNodeIds, List<String> steps, List<String> risks)
        implements AnnotationPayload {
    public ControlFlowPlanPayload {
        affectedNodeIds = copyNonempty(affectedNodeIds, "affectedNodeIds", true);
        steps = copyNonempty(steps, "steps", false);
        risks = copyNonempty(risks, "risks", false);
    }

    private static List<String> copyNonempty(List<String> values, String field, boolean hashes) {
        if (values == null || values.isEmpty()) throw new IllegalArgumentException(field + " must not be empty");
        return values.stream().map(value -> hashes
                ? AnnotatedContract.hash(value, field)
                : AnnotatedContract.text(value, field)).toList();
    }
}
