package org.shark.renovatio.cobol.recipes.annotate;

import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolContext;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.shark.renovatio.cobol.ir.annotated.AnnotationProvenance;
import org.shark.renovatio.cobol.ir.annotated.AnnotationReview;
import org.shark.renovatio.cobol.ir.annotated.CobolAnnotation;
import org.shark.renovatio.cobol.ir.annotated.CobolIrIdentityProjector;
import org.shark.renovatio.cobol.ir.annotated.DataIntentPayload;
import org.shark.renovatio.cobol.ir.annotated.DomainNamingPayload;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.ir.parser.SimpleCobolIrParser;

import java.time.Instant;
import java.util.List;

/** Builds {@link CobolIntermediateModel} + {@link AnnotatedCobolModel} pairs for applicator tests. */
final class AnnotatedFixtures {

    static final String COBOL = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. SAMPLE.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(30).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 'A' TO CUSTOMER-NAME.
            """;

    private AnnotatedFixtures() {
    }

    record Fixture(CobolIntermediateModel model, AnnotatedCobolModel sidecar) {
        AnnotatedCobolContext context() {
            return new AnnotatedCobolContext(model, sidecar);
        }
    }

    static CobolIntermediateModel model() {
        return new SimpleCobolIrParser().parse(COBOL);
    }

    static String dataItemNodeId(CobolIntermediateModel model) {
        return new CobolIrIdentityProjector().nodes(model).stream()
                .filter(n -> n.nodeKind() == AnnotatedNodeKind.DATA_ITEM)
                .findFirst().orElseThrow().nodeId();
    }

    static String baseHash(CobolIntermediateModel model) {
        return new CobolIrIdentityProjector().baseIrHash(model);
    }

    private static AnnotationProvenance provenance() {
        return new AnnotationProvenance("offline", "fake", "cobol.domain.naming", "v1",
                "domain-naming.v1", "1".repeat(64), "0".repeat(64),
                "tool-20260830t12345678901234z", AnnotationProvenance.CacheDisposition.MISS);
    }

    private static AnnotationReview review(AnnotationReview.ReviewState state) {
        return switch (state) {
            case ACCEPTED, REJECTED ->
                    new AnnotationReview(state, null, "reviewer", Instant.parse("2026-01-01T00:00:00Z"));
            case NEEDS_REVIEW -> new AnnotationReview(state, "reviewer", null, null);
            case PROPOSED -> new AnnotationReview(state, null, null, null);
        };
    }

    static CobolAnnotation domainNamingAnnotation(CobolIntermediateModel model, String suggestedName,
                                                  AnnotationReview.ReviewState state) {
        return new CobolAnnotation("a".repeat(64), dataItemNodeId(model), AnnotatedNodeKind.DATA_ITEM,
                AnnotationFamily.DOMAIN_NAMING,
                new DomainNamingPayload(suggestedName, "Customers", "rename for clarity"),
                0.9, provenance(), review(state));
    }

    static CobolAnnotation dataIntentAnnotation(CobolIntermediateModel model,
                                                AnnotationReview.ReviewState state) {
        return new CobolAnnotation("b".repeat(64), dataItemNodeId(model), AnnotatedNodeKind.DATA_ITEM,
                AnnotationFamily.DATA_INTENT,
                new DataIntentPayload(DataIntentPayload.Construction.REDEFINES,
                        "overlay of the raw record", List.of("caller sets exactly one branch")),
                0.75, provenance(), review(state));
    }

    static Fixture sidecar(CobolIntermediateModel model, String baseHash, List<CobolAnnotation> annotations) {
        return new Fixture(model, new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                "cobol-ir.v1", baseHash, annotations));
    }

    static Fixture domainNaming(String suggestedName, AnnotationReview.ReviewState state) {
        CobolIntermediateModel model = model();
        return sidecar(model, baseHash(model), List.of(domainNamingAnnotation(model, suggestedName, state)));
    }

    static Fixture domainNaming(String suggestedName) {
        return domainNaming(suggestedName, AnnotationReview.ReviewState.ACCEPTED);
    }

    static Fixture redefinesDataIntent() {
        CobolIntermediateModel model = model();
        return sidecar(model, baseHash(model),
                List.of(dataIntentAnnotation(model, AnnotationReview.ReviewState.ACCEPTED)));
    }
}
