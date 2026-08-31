package org.shark.renovatio.provider.cobol.translation;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolContext;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolValidator;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.CobolIrIdentityProjector;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.recipes.PopulateCobolProcessRecipe;
import org.shark.renovatio.cobol.recipes.annotate.AnnotationOutcomeKey;
import org.shark.renovatio.cobol.recipes.annotate.DroppedAnnotation;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItem;
import org.shark.renovatio.provider.java.OpenRewriteRunResult;
import org.shark.renovatio.provider.java.OpenRewriteRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
public class CobolSemanticTranspiler {

    private final OpenRewriteRunner runner;
    private final AnnotationActionItemFactory actionItemFactory = new AnnotationActionItemFactory();

    public CobolSemanticTranspiler(OpenRewriteRunner runner) {
        this.runner = runner;
    }

    public String enrichServiceImplementation(String javaSource, CobolIntermediateModel model) {
        return enrichServiceImplementation(javaSource, model, null);
    }

    public String enrichServiceImplementation(String javaSource, AnnotatedCobolContext annotatedContext) {
        if (annotatedContext == null) return javaSource;
        return enrichServiceImplementation(javaSource, annotatedContext, ignored -> { });
    }

    public String enrichServiceImplementation(String javaSource, AnnotatedCobolContext annotatedContext,
                                              Consumer<List<ManualActionItem>> sink) {
        if (annotatedContext == null) return javaSource;
        return enrichServiceImplementation(javaSource, annotatedContext.baseModel(), annotatedContext,
                sink == null ? ignored -> { } : sink);
    }

    private String enrichServiceImplementation(String javaSource, CobolIntermediateModel model,
                                                AnnotatedCobolContext annotatedContext) {
        return enrichServiceImplementation(javaSource, model, annotatedContext, ignored -> { });
    }

    private String enrichServiceImplementation(String javaSource, CobolIntermediateModel model,
                                                AnnotatedCobolContext annotatedContext,
                                                Consumer<List<ManualActionItem>> sink) {
        if (javaSource == null || javaSource.isBlank() || model == null) {
            return javaSource;
        }
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        ctx.putMessage(PopulateCobolProcessRecipe.CONTEXT_KEY, model);
        if (annotatedContext != null && annotatedContext.baseModel() == model && isValid(annotatedContext)) {
            ctx.putMessage(PopulateCobolProcessRecipe.ANNOTATED_CONTEXT_KEY, annotatedContext);
        }

        JavaParser javaParser = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build();
        List<SourceFile> sources = javaParser.parse(ctx, javaSource).collect(java.util.stream.Collectors.toList());

        OpenRewriteRunResult runResult = runner.runRecipe(new PopulateCobolProcessRecipe(), ctx, sources);
        drainAnnotationOutcomes(ctx, model, sink);
        if (!runResult.getValidationErrors().isEmpty() || runResult.getResults().isEmpty()) {
            return javaSource;
        }
        Result first = runResult.getResults().get(0);
        return first.getAfter() != null ? first.getAfter().printAll() : javaSource;
    }

    private void drainAnnotationOutcomes(ExecutionContext ctx, CobolIntermediateModel model,
                                         Consumer<List<ManualActionItem>> sink) {
        List<DroppedAnnotation> dropped = ctx.getMessage(AnnotationOutcomeKey.ANNOTATION_OUTCOMES_KEY);
        if (dropped == null || dropped.isEmpty()) {
            return;
        }
        String program = model.getProgramId();
        List<ManualActionItem> items = dropped.stream()
                .map(item -> actionItemFactory.toActionItem(item, program + ".cob", program))
                .sorted()
                .toList();
        sink.accept(items);
    }

    private boolean isValid(AnnotatedCobolContext context) {
        CobolIrIdentityProjector projector = new CobolIrIdentityProjector();
        if (!CobolIrIdentityProjector.BASE_IR_VERSION.equals(context.sidecar().baseIrVersion())) return false;
        Map<String, AnnotatedNodeKind> nodes = projector.nodes(context.baseModel()).stream()
                .collect(Collectors.toUnmodifiableMap(
                        CobolIrIdentityProjector.ProjectedNode::nodeId,
                        CobolIrIdentityProjector.ProjectedNode::nodeKind));
        return new AnnotatedCobolValidator().validate(
                context.sidecar(), projector.baseIrHash(context.baseModel()), nodes).isEmpty();
    }
}
