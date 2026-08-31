package org.shark.renovatio.cobol.recipes.annotate;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.ChangeFieldName;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.shark.renovatio.cobol.ir.annotated.AnnotationReview;
import org.shark.renovatio.cobol.ir.annotated.CobolAnnotation;
import org.shark.renovatio.cobol.ir.annotated.CobolIrIdentityProjector;
import org.shark.renovatio.cobol.ir.annotated.DataIntentPayload;
import org.shark.renovatio.cobol.ir.annotated.DomainNamingPayload;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Applies validated, {@code ACCEPTED} sidecar annotations to a generated {@link J.CompilationUnit}
 * using AST-safe transformations only. Ineligible annotations are reported as {@link DroppedAnnotation}
 * records; the tree is never left partially transformed.
 *
 * <p>The caller has already validated the sidecar (schema + semantic validator). This class only
 * re-checks the base-IR hash, the review state, the applied-family set, and per-node resolution.
 */
public final class AnnotationApplicator {

    private static final String DATA_INTENT_FQN = "org.shark.renovatio.cobol.annotations.CobolDataIntent";
    private static final Pattern JAVA_IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    private final AnnotatedCobolModel sidecar;
    private final NodeIdentityIndex index;
    private final boolean hashMatches;

    public AnnotationApplicator(CobolIntermediateModel model, AnnotatedCobolModel sidecar) {
        this.sidecar = sidecar;
        this.index = new NodeIdentityIndex(model);
        this.hashMatches = new CobolIrIdentityProjector().baseIrHash(model).equals(sidecar.baseIrHash());
    }

    List<CobolAnnotation> ordered() {
        List<CobolAnnotation> list = new ArrayList<>(sidecar.annotations());
        list.sort(Comparator.comparing(CobolAnnotation::nodeId).thenComparing(CobolAnnotation::annotationId));
        return list;
    }

    private static boolean isAppliedFamily(AnnotationFamily family) {
        return family == AnnotationFamily.DOMAIN_NAMING || family == AnnotationFamily.DATA_INTENT;
    }

    /** Annotations eligible for AST application, in deterministic {@code (nodeId, annotationId)} order. */
    List<CobolAnnotation> eligible() {
        List<CobolAnnotation> out = new ArrayList<>();
        if (!hashMatches) {
            return out;
        }
        for (CobolAnnotation a : ordered()) {
            if (a.review().reviewState() != AnnotationReview.ReviewState.ACCEPTED) {
                continue;
            }
            if (!isAppliedFamily(a.annotationFamily())) {
                continue;
            }
            if (index.resolve(a.nodeId(), a.nodeKind()).isEmpty()) {
                continue;
            }
            out.add(a);
        }
        return out;
    }

    public AnnotationApplicationOutcome apply(J.CompilationUnit cu, ExecutionContext ctx) {
        List<DroppedAnnotation> dropped = new ArrayList<>();
        for (CobolAnnotation a : ordered()) {
            classifyDrop(a).ifPresent(dropped::add);
        }
        J.CompilationUnit tree = cu;
        if (tree != null) {
            for (CobolAnnotation a : eligible()) {
                NodeIdentityIndex.Resolved resolved = index.resolve(a.nodeId(), a.nodeKind()).orElseThrow();
                if (a.annotationFamily() == AnnotationFamily.DATA_INTENT) {
                    tree = applyDataIntent(tree, ctx, a,
                            NodeIdentityIndex.toJavaFieldName(resolved.cobolName()));
                } else if (a.annotationFamily() == AnnotationFamily.DOMAIN_NAMING) {
                    tree = applyDomainNaming(tree, ctx, a, resolved, dropped);
                }
            }
        }
        return new AnnotationApplicationOutcome(tree, dropped);
    }

    private J.CompilationUnit applyDomainNaming(J.CompilationUnit cu, ExecutionContext ctx,
                                                CobolAnnotation annotation,
                                                NodeIdentityIndex.Resolved resolved,
                                                List<DroppedAnnotation> dropped) {
        DomainNamingPayload payload = (DomainNamingPayload) annotation.payload();
        String target = payload.suggestedName();
        if (!JAVA_IDENTIFIER.matcher(target).matches() || identifierInUse(cu, target)) {
            dropped.add(new DroppedAnnotation(annotation.nodeId(), annotation.annotationId(),
                    annotation.annotationFamily(), DroppedAnnotation.DropReason.NAME_COLLISION,
                    payload.rationale()));
            return cu;
        }

        if (resolved.kind() == org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind.PARAGRAPH) {
            return renameMethods(cu, ctx, NodeIdentityIndex.toJavaMethodName(resolved.cobolName()), target);
        }

        String currentField = NodeIdentityIndex.toJavaFieldName(resolved.cobolName());
        String oldStem = accessorStem(currentField);
        String newStem = accessorStem(target);
        String declaringType = declaringType(cu, currentField);
        if (declaringType == null) {
            if (methodNameInUse(cu, "get" + oldStem) || methodNameInUse(cu, "set" + oldStem)) {
                J.CompilationUnit renamed = renameMethods(cu, ctx, "get" + oldStem, "get" + newStem);
                return renameMethods(renamed, ctx, "set" + oldStem, "set" + newStem);
            }
            dropped.add(new DroppedAnnotation(annotation.nodeId(), annotation.annotationId(),
                    annotation.annotationFamily(), DroppedAnnotation.DropReason.NODE_UNRESOLVED,
                    "Generated field " + currentField + " was not found"));
            return cu;
        }

        J.CompilationUnit renamed = (J.CompilationUnit) new ChangeFieldName<ExecutionContext>(
                declaringType, currentField, target).visit(cu, ctx);
        renamed = renameMethods(renamed, ctx, "get" + oldStem, "get" + newStem);
        return renameMethods(renamed, ctx, "set" + oldStem, "set" + newStem);
    }

    private static String accessorStem(String javaIdentifier) {
        return Character.toUpperCase(javaIdentifier.charAt(0)) + javaIdentifier.substring(1);
    }

    private static boolean identifierInUse(J.CompilationUnit cu, String candidate) {
        AtomicBoolean found = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean state) {
                J.Identifier visited = super.visitIdentifier(identifier, state);
                if (visited.getSimpleName().equals(candidate)) {
                    state.set(true);
                }
                return visited;
            }
        }.visit(cu, found);
        return found.get();
    }

    private static boolean methodNameInUse(J.CompilationUnit cu, String candidate) {
        AtomicBoolean found = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation invocation, AtomicBoolean state) {
                J.MethodInvocation visited = super.visitMethodInvocation(invocation, state);
                if (visited.getSimpleName().equalsIgnoreCase(candidate)) {
                    state.set(true);
                }
                return visited;
            }
        }.visit(cu, found);
        return found.get();
    }

    private static String declaringType(J.CompilationUnit cu, String fieldName) {
        AtomicReference<String> result = new AtomicReference<>();
        new JavaIsoVisitor<AtomicReference<String>>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations declarations,
                                                                    AtomicReference<String> state) {
                J.VariableDeclarations visited = super.visitVariableDeclarations(declarations, state);
                boolean field = getCursor().firstEnclosing(J.ClassDeclaration.class) != null
                        && getCursor().firstEnclosing(J.MethodDeclaration.class) == null;
                if (field && visited.getVariables().stream()
                        .anyMatch(variable -> variable.getSimpleName().equals(fieldName))) {
                    J.ClassDeclaration owner = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    if (owner != null && owner.getType() != null) {
                        state.compareAndSet(null, owner.getType().getFullyQualifiedName());
                    }
                }
                return visited;
            }
        }.visit(cu, result);
        return result.get();
    }

    private static J.CompilationUnit renameMethods(J.CompilationUnit cu, ExecutionContext ctx,
                                                   String from, String to) {
        return (J.CompilationUnit) new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
                                                              ExecutionContext context) {
                J.MethodDeclaration visited = super.visitMethodDeclaration(method, context);
                if (!visited.getSimpleName().equalsIgnoreCase(from)) {
                    return visited;
                }
                JavaType.Method type = visited.getMethodType();
                return visited.withName(visited.getName().withSimpleName(to))
                        .withMethodType(type == null ? null : type.withName(to));
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation invocation,
                                                            ExecutionContext context) {
                J.MethodInvocation visited = super.visitMethodInvocation(invocation, context);
                if (!visited.getSimpleName().equalsIgnoreCase(from)) {
                    return visited;
                }
                JavaType.Method type = visited.getMethodType();
                return visited.withName(visited.getName().withSimpleName(to))
                        .withMethodType(type == null ? null : type.withName(to));
            }
        }.visit(cu, ctx);
    }

    private J.CompilationUnit applyDataIntent(J.CompilationUnit cu, ExecutionContext ctx,
                                              CobolAnnotation a, String fieldName) {
        DataIntentPayload payload = (DataIntentPayload) a.payload();
        String assumptions = payload.assumptions().stream()
                .map(AnnotationApplicator::quote)
                .collect(Collectors.joining(", ", "{", "}"));
        String annotation = String.format(Locale.ROOT,
                "@CobolDataIntent(nodeId = %s, annotationId = %s, "
                        + "construction = CobolDataIntent.Construction.%s, interpretation = %s, assumptions = %s)",
                quote(a.nodeId()), quote(a.annotationId()), payload.construction().name(),
                quote(payload.interpretation()), assumptions);

        return (J.CompilationUnit) new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable,
                                                                    ExecutionContext context) {
                J.VariableDeclarations vd = super.visitVariableDeclarations(multiVariable, context);
                boolean isField = getCursor().firstEnclosing(J.ClassDeclaration.class) != null
                        && getCursor().firstEnclosing(J.MethodDeclaration.class) == null;
                boolean nameMatches = !vd.getVariables().isEmpty()
                        && vd.getVariables().get(0).getSimpleName().equals(fieldName);
                boolean alreadyAnnotated = vd.getLeadingAnnotations().stream()
                        .anyMatch(an -> "CobolDataIntent".equals(an.getSimpleName()));
                if (!isField || !nameMatches || alreadyAnnotated) {
                    return vd;
                }
                maybeAddImport(DATA_INTENT_FQN);
                return JavaTemplate.builder(annotation)
                        .imports(DATA_INTENT_FQN)
                        .javaParser(JavaParser.fromJavaVersion().classpath("renovatio-cobol-annotations"))
                        .build()
                        .apply(getCursor(), vd.getCoordinates().addAnnotation(
                                Comparator.comparing(J.Annotation::getSimpleName)));
            }
        }.visit(cu, ctx);
    }

    private static String quote(String value) {
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private Optional<DroppedAnnotation> classifyDrop(CobolAnnotation a) {
        if (!hashMatches) {
            return drop(a, DroppedAnnotation.DropReason.STALE_SIDECAR);
        }
        if (!isAppliedFamily(a.annotationFamily())) {
            return drop(a, DroppedAnnotation.DropReason.FAMILY_NOT_APPLIED);
        }
        return switch (a.review().reviewState()) {
            case REJECTED -> drop(a, DroppedAnnotation.DropReason.REJECTED);
            case PROPOSED, NEEDS_REVIEW -> drop(a, DroppedAnnotation.DropReason.PENDING_REVIEW);
            case ACCEPTED -> index.resolve(a.nodeId(), a.nodeKind()).isEmpty()
                    ? drop(a, DroppedAnnotation.DropReason.NODE_UNRESOLVED)
                    : Optional.empty();
        };
    }

    private static Optional<DroppedAnnotation> drop(CobolAnnotation a, DroppedAnnotation.DropReason reason) {
        return Optional.of(new DroppedAnnotation(a.nodeId(), a.annotationId(), a.annotationFamily(),
                reason, a.annotationFamily().name()));
    }
}
