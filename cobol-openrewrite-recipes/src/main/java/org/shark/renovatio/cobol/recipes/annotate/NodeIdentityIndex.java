package org.shark.renovatio.cobol.recipes.annotate;

import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.CobolIrIdentityProjector;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves an annotation {@code nodeId} to the COBOL name of the base node it addresses, so the
 * {@link AnnotationApplicator} can map it onto a generated Java identifier. Backed by
 * {@link CobolIrIdentityProjector#nodes(CobolIntermediateModel)}; never mutates the model.
 */
public final class NodeIdentityIndex {

    /** A resolved base node: its COBOL name, its kind, and its canonical JSON pointer. */
    public record Resolved(String cobolName, AnnotatedNodeKind kind, String pointer) {}

    private final Map<String, CobolIrIdentityProjector.ProjectedNode> byId = new LinkedHashMap<>();

    public NodeIdentityIndex(CobolIntermediateModel model) {
        for (CobolIrIdentityProjector.ProjectedNode node : new CobolIrIdentityProjector().nodes(model)) {
            byId.putIfAbsent(node.nodeId(), node);
        }
    }

    /**
     * Resolves {@code nodeId} only when it is present and its kind equals {@code expectedKind}.
     * Returns empty otherwise.
     */
    public Optional<Resolved> resolve(String nodeId, AnnotatedNodeKind expectedKind) {
        CobolIrIdentityProjector.ProjectedNode node = byId.get(nodeId);
        if (node == null || node.nodeKind() != expectedKind) {
            return Optional.empty();
        }
        return Optional.of(new Resolved(cobolName(node), node.nodeKind(), node.pointer()));
    }

    private static String cobolName(CobolIrIdentityProjector.ProjectedNode node) {
        Object name = node.semanticContent().get("name");
        if (name instanceof String text && !text.isBlank()) {
            return text;
        }
        return lastPointerSegment(node.pointer());
    }

    private static String lastPointerSegment(String pointer) {
        int slash = pointer.lastIndexOf('/');
        String segment = slash >= 0 ? pointer.substring(slash + 1) : pointer;
        return segment.replace("~1", "/").replace("~0", "~");
    }

    /** {@code CUSTOMER-NAME} &rarr; {@code CustomerName}: the stem shared by getters and setters. */
    public static String toJavaAccessorStem(String cobolName) {
        StringBuilder sb = new StringBuilder();
        for (String part : cobolName.replace('.', ' ').replace('-', ' ').trim().split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0)))
              .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    /** {@code CUSTOMER-NAME} &rarr; {@code customerName}: the generated field name. */
    public static String toJavaFieldName(String cobolName) {
        String stem = toJavaAccessorStem(cobolName);
        return stem.isEmpty() ? stem : Character.toLowerCase(stem.charAt(0)) + stem.substring(1);
    }

    /** {@code MAIN-PARA} &rarr; {@code mainPara}: the generated service method name. */
    public static String toJavaMethodName(String cobolName) {
        return toJavaFieldName(cobolName);
    }
}
