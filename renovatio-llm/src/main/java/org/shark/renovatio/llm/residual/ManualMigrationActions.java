package org.shark.renovatio.llm.residual;

import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.shark.renovatio.cobol.ir.annotated.CobolAnnotation;
import org.shark.renovatio.cobol.ir.annotated.UnsupportedExplanationPayload;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable content-addressed set of manual migration work, naturally deduplicated by action ID. */
public final class ManualMigrationActions {
    private final Map<String, ManualMigrationAction> entries;

    public ManualMigrationActions() {
        this(Map.of());
    }

    private ManualMigrationActions(Map<String, ManualMigrationAction> entries) {
        this.entries = Map.copyOf(entries);
    }

    public ManualMigrationActions add(ManualMigrationAction action) {
        Objects.requireNonNull(action, "action");
        Map<String, ManualMigrationAction> updated = new LinkedHashMap<>(entries);
        updated.putIfAbsent(action.actionId(), action);
        return new ManualMigrationActions(updated);
    }

    public ManualMigrationActions addUnsupported(CobolAnnotation annotation, String diagnosticCode,
                                                  String evidenceRequired) {
        Objects.requireNonNull(annotation, "annotation");
        if (annotation.annotationFamily() != AnnotationFamily.UNSUPPORTED_EXPLANATION
                || !(annotation.payload() instanceof UnsupportedExplanationPayload payload)) {
            throw new IllegalArgumentException("annotation is not an unsupported explanation");
        }
        ManualMigrationAction action = ManualMigrationAction.create(annotation.nodeId(),
                payload.construction(), payload.explanation(),
                "Behavior for " + payload.construction() + " at node " + annotation.nodeId()
                        + " is not represented by deterministic translation.",
                payload.manualAction(), evidenceRequired, diagnosticCode,
                annotation.provenance().toolRunRef());
        return add(action);
    }

    public List<ManualMigrationAction> entries() {
        List<ManualMigrationAction> sorted = new ArrayList<>(entries.values());
        sorted.sort(null);
        return List.copyOf(sorted);
    }
}
