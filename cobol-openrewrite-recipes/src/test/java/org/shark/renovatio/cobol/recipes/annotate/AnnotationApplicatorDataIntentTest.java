package org.shark.renovatio.cobol.recipes.annotate;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.shark.renovatio.cobol.ir.annotated.CobolAnnotation;
import org.shark.renovatio.cobol.ir.annotated.DataIntentPayload;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceSpan;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationApplicatorDataIntentTest {

    private static final String DTO = """
            package sample;
            public class SampleDto {
                private String customerName;
                public String getCustomerName() { return customerName; }
                public void setCustomerName(String v) { this.customerName = v; }
            }
            """;

    private J.CompilationUnit parse(ExecutionContext ctx) {
        return JavaParser.fromJavaVersion().build()
                .parse(ctx, DTO)
                .map(J.CompilationUnit.class::cast)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void attachesCobolDataIntentAnnotationToField() {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        J.CompilationUnit cu = parse(ctx);
        AnnotatedFixtures.Fixture f = AnnotatedFixtures.redefinesDataIntent();

        AnnotationApplicationOutcome outcome =
                new AnnotationApplicator(f.model(), f.sidecar()).apply(cu, ctx);

        String printed = outcome.tree().printAll();
        assertThat(printed).contains("@CobolDataIntent(");
        assertThat(printed).contains("CobolDataIntent.Construction.REDEFINES");
        assertThat(printed).contains("private String customerName");
        assertThat(outcome.dropped()).isEmpty();
    }

    @Test
    void isDeterministicAcrossRuns() {
        ExecutionContext ctx1 = new InMemoryExecutionContext(Throwable::printStackTrace);
        ExecutionContext ctx2 = new InMemoryExecutionContext(Throwable::printStackTrace);
        AnnotatedFixtures.Fixture f = AnnotatedFixtures.redefinesDataIntent();

        String a = new AnnotationApplicator(f.model(), f.sidecar()).apply(parse(ctx1), ctx1).tree().printAll();
        String b = new AnnotationApplicator(f.model(), f.sidecar()).apply(parse(ctx2), ctx2).tree().printAll();

        assertThat(a).isEqualTo(b);
    }

    @Test
    void neutralIntentPreservesExactLegacyAnnotationBytes() {
        AnnotatedFixtures.Fixture fixture = AnnotatedFixtures.redefinesDataIntent();
        CobolAnnotation source = fixture.sidecar().annotations().get(0);
        DataIntentPayload payload = (DataIntentPayload) source.payload();
        SemanticProgram.DataIntent neutral = new SemanticProgram.DataIntent(
                SemanticProgram.Header.create(fixture.model().getProgramId(),
                        SemanticProgram.NodeKind.DATA_INTENT, "data-intent:" + source.annotationId(),
                        new SourceSpan("input.cob", 1, 1, 1, 1)),
                "7".repeat(64), SemanticProgram.IntentKind.OVERLAPPING_STORAGE,
                payload.interpretation(), payload.assumptions(), source.annotationId());

        ExecutionContext legacyContext = new InMemoryExecutionContext(Throwable::printStackTrace);
        String legacy = new AnnotationApplicator(fixture.model(), fixture.sidecar())
                .apply(parse(legacyContext), legacyContext).tree().printAll();
        ExecutionContext neutralContext = new InMemoryExecutionContext(Throwable::printStackTrace);
        String actual = new AnnotationApplicator(fixture.model(), fixture.sidecar(), List.of(neutral))
                .apply(parse(neutralContext), neutralContext).tree().printAll();

        assertThat(actual).isEqualTo(legacy);
    }

    @Test
    void appliesDataIntentBeforeDomainRenameOnTheSameNode() {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        CobolIntermediateModel model = AnnotatedFixtures.model();
        AnnotatedFixtures.Fixture fixture = AnnotatedFixtures.sidecar(model,
                AnnotatedFixtures.baseHash(model), List.of(
                        AnnotatedFixtures.domainNamingAnnotation(model, "clientFullName",
                                org.shark.renovatio.cobol.ir.annotated.AnnotationReview.ReviewState.ACCEPTED),
                        AnnotatedFixtures.dataIntentAnnotation(model,
                                org.shark.renovatio.cobol.ir.annotated.AnnotationReview.ReviewState.ACCEPTED)));

        AnnotationApplicationOutcome outcome =
                new AnnotationApplicator(fixture.model(), fixture.sidecar()).apply(parse(ctx), ctx);

        assertThat(outcome.tree().printAll())
                .contains("@CobolDataIntent(")
                .contains("private String clientFullName");
        assertThat(outcome.dropped()).isEmpty();
    }

    @Test
    void escapesJavaStringLiteralControlCharacters() {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        CobolIntermediateModel model = AnnotatedFixtures.model();
        CobolAnnotation original = AnnotatedFixtures.dataIntentAnnotation(model,
                org.shark.renovatio.cobol.ir.annotated.AnnotationReview.ReviewState.ACCEPTED);
        DataIntentPayload payload = new DataIntentPayload(DataIntentPayload.Construction.REDEFINES,
                "line one\nline two\rline three\tquoted \"value\"",
                List.of("path\\branch\nnext"));
        CobolAnnotation withControls = new CobolAnnotation(original.annotationId(), original.nodeId(),
                AnnotatedNodeKind.DATA_ITEM, AnnotationFamily.DATA_INTENT, payload, original.confidence(),
                original.provenance(), original.review());
        AnnotatedFixtures.Fixture fixture = AnnotatedFixtures.sidecar(model,
                AnnotatedFixtures.baseHash(model), List.of(withControls));

        AnnotationApplicationOutcome outcome =
                new AnnotationApplicator(fixture.model(), fixture.sidecar()).apply(parse(ctx), ctx);

        assertThat(outcome.tree().printAll())
                .contains("line one\\nline two\\rline three\\tquoted \\\"value\\\"")
                .contains("path\\\\branch\\nnext");
        assertThat(outcome.dropped()).isEmpty();
    }
}
