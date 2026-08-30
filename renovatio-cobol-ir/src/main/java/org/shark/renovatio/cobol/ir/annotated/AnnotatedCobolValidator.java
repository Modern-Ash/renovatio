package org.shark.renovatio.cobol.ir.annotated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Deterministic cross-document validation performed after JSON Schema validation. */
public final class AnnotatedCobolValidator {

    public List<AnnotatedValidationDiagnostic> validate(AnnotatedCobolModel sidecar, String baseIrHash,
                                                         Map<String, AnnotatedNodeKind> nodes) {
        Objects.requireNonNull(sidecar, "sidecar");
        baseIrHash = AnnotatedContract.hash(baseIrHash, "baseIrHash");
        nodes = Map.copyOf(Objects.requireNonNull(nodes, "nodes"));
        List<AnnotatedValidationDiagnostic> diagnostics = new ArrayList<>();

        if (!AnnotatedCobolModel.SCHEMA_VERSION.equals(sidecar.schemaVersion())) {
            add(diagnostics, AnnotatedValidationDiagnostic.Code.ANNOTATED_IR_UNSUPPORTED_VERSION,
                    "/schemaVersion", "Unsupported annotated IR schema version: " + sidecar.schemaVersion());
        }
        if (!baseIrHash.equals(sidecar.baseIrHash())) {
            add(diagnostics, AnnotatedValidationDiagnostic.Code.ANNOTATED_IR_BASE_HASH_MISMATCH,
                    "/baseIrHash", "Sidecar does not reference the supplied base IR");
        }

        Map<String, Integer> firstIdentity = new HashMap<>();
        Map<String, String> firstOutput = new HashMap<>();
        for (int index = 0; index < sidecar.annotations().size(); index++) {
            CobolAnnotation annotation = sidecar.annotations().get(index);
            String root = "/annotations/" + index;
            String expectedIdentity = AnnotatedIdentity.annotationId(annotation.nodeId(),
                    annotation.annotationFamily(), annotation.provenance());
            if (!expectedIdentity.equals(annotation.annotationId())) {
                add(diagnostics, AnnotatedValidationDiagnostic.Code.ANNOTATED_IR_DUPLICATE_IDENTITY,
                        root + "/annotationId", "Annotation identity does not match its canonical projection");
            }
            String expectedOutput = AnnotatedIdentity.outputHash(annotation.annotationFamily(),
                    annotation.payload(), annotation.confidence());
            if (!expectedOutput.equals(annotation.provenance().outputHash())) {
                add(diagnostics, AnnotatedValidationDiagnostic.Code.ANNOTATED_IR_NONDETERMINISTIC_OUTPUT,
                        root + "/provenance/outputHash", "Output hash does not match the validated proposal projection");
            }
            AnnotatedNodeKind actualKind = nodes.get(annotation.nodeId());
            if (actualKind == null) {
                add(diagnostics, AnnotatedValidationDiagnostic.Code.ANNOTATED_IR_NODE_UNRESOLVED,
                        root + "/nodeId", "Annotation node does not resolve in the base IR");
            } else if (actualKind != annotation.nodeKind()) {
                add(diagnostics, AnnotatedValidationDiagnostic.Code.ANNOTATED_IR_NODE_KIND_MISMATCH,
                        root + "/nodeKind", "Annotation node kind does not match the base IR node");
            }

            Integer previous = firstIdentity.putIfAbsent(annotation.annotationId(), index);
            String previousOutput = firstOutput.putIfAbsent(annotation.annotationId(), annotation.provenance().outputHash());
            if (previous != null) {
                add(diagnostics, AnnotatedValidationDiagnostic.Code.ANNOTATED_IR_DUPLICATE_IDENTITY,
                        root + "/annotationId", "Annotation identity duplicates /annotations/" + previous);
                if (!previousOutput.equals(annotation.provenance().outputHash())) {
                    add(diagnostics, AnnotatedValidationDiagnostic.Code.ANNOTATED_IR_NONDETERMINISTIC_OUTPUT,
                            root + "/provenance/outputHash", "One annotation identity has conflicting validated outputs");
                }
            }

            if (annotation.payload() instanceof ControlFlowPlanPayload plan) {
                for (int affected = 0; affected < plan.affectedNodeIds().size(); affected++) {
                    if (!nodes.containsKey(plan.affectedNodeIds().get(affected))) {
                        add(diagnostics, AnnotatedValidationDiagnostic.Code.ANNOTATED_IR_NODE_UNRESOLVED,
                                root + "/payload/affectedNodeIds/" + affected,
                                "Affected control-flow node does not resolve in the base IR");
                    }
                }
            }
        }
        diagnostics.sort(null);
        return List.copyOf(diagnostics);
    }

    private static void add(List<AnnotatedValidationDiagnostic> diagnostics,
                            AnnotatedValidationDiagnostic.Code code, String pointer, String message) {
        diagnostics.add(new AnnotatedValidationDiagnostic(code, pointer, message));
    }
}
