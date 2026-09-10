package org.shark.renovatio.cobol.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolContext;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.shark.renovatio.cobol.ir.annotated.AnnotationProvenance;
import org.shark.renovatio.cobol.ir.annotated.AnnotationReview;
import org.shark.renovatio.cobol.ir.annotated.CobolAnnotation;
import org.shark.renovatio.cobol.ir.annotated.CobolIrIdentityProjector;
import org.shark.renovatio.cobol.ir.annotated.DomainNamingPayload;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.ir.parser.SimpleCobolIrParser;
import org.shark.renovatio.cobol.recipes.annotate.AnnotationOutcomeKey;
import org.shark.renovatio.cobol.recipes.annotate.DroppedAnnotation;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PopulateCobolProcessRecipeAnnotatedTest {

    private static final String COBOL = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. SAMPLE.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(30).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 'A' TO CUSTOMER-NAME.
            """;

    private static final String JAVA_STUB = """
            package sample;
            public class SampleDto {
                private String customerName;
                public String getCustomerName() { return customerName; }
                public void setCustomerName(String v) { this.customerName = v; }
                public SampleDto process(SampleDto input) {
                    SampleDto output = new SampleDto();
                    return output;
                }
            }
            """;

    @Test
    void appliesAcceptedRenameAndRecordsDroppedAnnotations() {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        CobolIntermediateModel model = new SimpleCobolIrParser().parse(COBOL);
        AnnotatedCobolModel sidecar = sidecar(model);
        ctx.putMessage(PopulateCobolProcessRecipe.CONTEXT_KEY, model);
        ctx.putMessage(PopulateCobolProcessRecipe.ANNOTATED_CONTEXT_KEY,
                new AnnotatedCobolContext(model, sidecar));

        String after = run(ctx);

        assertThat(after).contains("private String clientFullName");
        assertThat(after).contains("setClientFullName(\"A\")");
        List<DroppedAnnotation> dropped = ctx.getMessage(AnnotationOutcomeKey.ANNOTATION_OUTCOMES_KEY);
        assertThat(dropped).isNotNull().anySatisfy(item ->
                assertThat(item.reason()).isEqualTo(DroppedAnnotation.DropReason.REJECTED));
    }

    @Test
    void legacyPathDoesNotPublishAnnotationOutcomes() {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        ctx.putMessage(PopulateCobolProcessRecipe.CONTEXT_KEY, new SimpleCobolIrParser().parse(COBOL));

        String after = run(ctx);

        assertThat(after).contains("setCustomerName(\"A\")");
        assertThat(ctx.<Object>getMessage(AnnotationOutcomeKey.ANNOTATION_OUTCOMES_KEY)).isNull();
    }

    private String run(ExecutionContext ctx) {
        List<SourceFile> sources = JavaParser.fromJavaVersion().build()
                .parse(ctx, JAVA_STUB).map(SourceFile.class::cast).toList();
        var run = new PopulateCobolProcessRecipe().run(new InMemoryLargeSourceSet(sources), ctx);
        return run.getChangeset().getAllResults().get(0).getAfter().printAll();
    }

    private AnnotatedCobolModel sidecar(CobolIntermediateModel model) {
        CobolIrIdentityProjector projector = new CobolIrIdentityProjector();
        String nodeId = projector.nodes(model).stream()
                .filter(node -> node.nodeKind() == AnnotatedNodeKind.DATA_ITEM)
                .findFirst().orElseThrow().nodeId();
        AnnotationProvenance provenance = new AnnotationProvenance("offline", "fake",
                "cobol.domain.naming", "v1", "domain-naming.v1", "1".repeat(64),
                "0".repeat(64), "tool-20260830t12345678901234z",
                AnnotationProvenance.CacheDisposition.MISS);
        CobolAnnotation accepted = new CobolAnnotation("a".repeat(64), nodeId,
                AnnotatedNodeKind.DATA_ITEM, AnnotationFamily.DOMAIN_NAMING,
                new DomainNamingPayload("clientFullName", "Customers", "accepted rename"), 0.9,
                provenance, new AnnotationReview(AnnotationReview.ReviewState.ACCEPTED, null,
                "reviewer", Instant.parse("2026-01-01T00:00:00Z")));
        CobolAnnotation rejected = new CobolAnnotation("b".repeat(64), nodeId,
                AnnotatedNodeKind.DATA_ITEM, AnnotationFamily.DOMAIN_NAMING,
                new DomainNamingPayload("ignoredName", "Customers", "rejected rename"), 0.4,
                provenance, new AnnotationReview(AnnotationReview.ReviewState.REJECTED, null,
                "reviewer", Instant.parse("2026-01-01T00:00:00Z")));
        return new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION, "cobol-ir.v1",
                projector.baseIrHash(model), List.of(rejected, accepted));
    }
}
