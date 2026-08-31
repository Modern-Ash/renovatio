package org.shark.renovatio.cobol.recipes.annotate;

import org.openrewrite.java.tree.J;

import java.util.List;

/** The result of an {@link AnnotationApplicator} pass: the (possibly rewritten) tree and every drop. */
public record AnnotationApplicationOutcome(J.CompilationUnit tree, List<DroppedAnnotation> dropped) {
}
