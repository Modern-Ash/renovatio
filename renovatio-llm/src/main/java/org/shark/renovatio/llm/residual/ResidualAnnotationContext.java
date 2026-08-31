package org.shark.renovatio.llm.residual;

import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.AnnotationProvenance;

import java.util.List;
import java.util.Objects;

/** Governed metadata required to turn one validated residual output into a typed sidecar entry. */
public record ResidualAnnotationContext(
        String baseIrVersion,
        String baseIrHash,
        String nodeId,
        AnnotatedNodeKind nodeKind,
        String provider,
        String model,
        String promptVersion,
        String outputSchemaVersion,
        String inputHash,
        String toolRunRef,
        AnnotationProvenance.CacheDisposition cacheDisposition,
        double confidence,
        String assignedHumanReviewer,
        List<String> affectedNodeIds,
        List<String> collisionScope,
        boolean publicSignatureProtected) {

    public ResidualAnnotationContext {
        requireText(baseIrVersion, "baseIrVersion");
        requireText(baseIrHash, "baseIrHash");
        requireText(nodeId, "nodeId");
        Objects.requireNonNull(nodeKind, "nodeKind");
        requireText(provider, "provider");
        requireText(model, "model");
        requireText(promptVersion, "promptVersion");
        requireText(outputSchemaVersion, "outputSchemaVersion");
        requireText(inputHash, "inputHash");
        requireText(toolRunRef, "toolRunRef");
        Objects.requireNonNull(cacheDisposition, "cacheDisposition");
        if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("confidence must be finite and between 0 and 1");
        }
        if (assignedHumanReviewer != null) requireText(assignedHumanReviewer, "assignedHumanReviewer");
        affectedNodeIds = List.copyOf(affectedNodeIds == null ? List.of() : affectedNodeIds);
        collisionScope = List.copyOf(collisionScope == null ? List.of() : collisionScope);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
