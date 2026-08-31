package org.shark.renovatio.llm.residual;

import com.fasterxml.jackson.databind.JsonNode;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedIdentity;
import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.shark.renovatio.cobol.ir.annotated.AnnotationPayload;
import org.shark.renovatio.cobol.ir.annotated.AnnotationProvenance;
import org.shark.renovatio.cobol.ir.annotated.AnnotationReview;
import org.shark.renovatio.cobol.ir.annotated.CobolAnnotation;
import org.shark.renovatio.cobol.ir.annotated.ControlFlowPlanPayload;
import org.shark.renovatio.cobol.ir.annotated.DataIntentPayload;
import org.shark.renovatio.cobol.ir.annotated.DomainNamingPayload;
import org.shark.renovatio.cobol.ir.annotated.UnsupportedExplanationPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Maps already schema-validated model output to the immutable annotated-IR v1 contract. */
public final class ResidualAnnotationAssembler {
    private final DomainNamingPolicy domainNamingPolicy = new DomainNamingPolicy();

    public AnnotatedCobolModel append(AnnotatedCobolModel sidecar, ResidualRoute route,
                                      JsonNode validatedOutput, ResidualAnnotationContext context) {
        Objects.requireNonNull(sidecar, "sidecar");
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(validatedOutput, "validatedOutput");
        Objects.requireNonNull(context, "context");
        if (!route.isResidual()) throw new IllegalArgumentException("deterministic output cannot become an annotation");
        if (!sidecar.baseIrVersion().equals(context.baseIrVersion())
                || !sidecar.baseIrHash().equals(context.baseIrHash())) {
            throw new IllegalArgumentException("annotation context does not reference the sidecar base IR");
        }

        AnnotationFamily family = family(route);
        AnnotationPayload payload = payload(route, validatedOutput, context);
        String outputHash = AnnotatedIdentity.outputHash(family, payload, context.confidence());
        AnnotationProvenance provenance = new AnnotationProvenance(context.provider(), context.model(),
                route.promptId(), context.promptVersion(), context.outputSchemaVersion(),
                context.inputHash(), outputHash, context.toolRunRef(), context.cacheDisposition());
        AnnotationReview review = review(route, context.assignedHumanReviewer());
        String annotationId = AnnotatedIdentity.annotationId(context.nodeId(), family, provenance);
        CobolAnnotation annotation = new CobolAnnotation(annotationId, context.nodeId(), context.nodeKind(),
                family, payload, context.confidence(), provenance, review);

        List<CobolAnnotation> annotations = new ArrayList<>(sidecar.annotations());
        for (CobolAnnotation existing : annotations) {
            if (!existing.annotationId().equals(annotationId)) continue;
            if (existing.provenance().outputHash().equals(outputHash)) return sidecar;
            throw new IllegalStateException("annotation identity conflicts with an existing proposal");
        }
        annotations.add(annotation);
        return new AnnotatedCobolModel(sidecar.schemaVersion(), sidecar.baseIrVersion(),
                sidecar.baseIrHash(), annotations);
    }

    private static AnnotationFamily family(ResidualRoute route) {
        return switch (route) {
            case DOMAIN_NAMING -> AnnotationFamily.DOMAIN_NAMING;
            case CONTROL_FLOW_PLAN -> AnnotationFamily.CONTROL_FLOW_PLAN;
            case REDEFINES_INTENT, OCCURS_DEPENDING_ON_INTENT -> AnnotationFamily.DATA_INTENT;
            case UNSUPPORTED_EXPLANATION -> AnnotationFamily.UNSUPPORTED_EXPLANATION;
            case DETERMINISTIC -> throw new IllegalArgumentException("deterministic route has no annotation family");
        };
    }

    private AnnotationPayload payload(ResidualRoute route, JsonNode output,
                                      ResidualAnnotationContext context) {
        return switch (route) {
            case DOMAIN_NAMING -> domainNamingPayload(output, context);
            case CONTROL_FLOW_PLAN -> new ControlFlowPlanPayload(context.affectedNodeIds(),
                    textList(output, "steps"), textList(output, "risks"));
            case REDEFINES_INTENT -> dataIntent(output, DataIntentPayload.Construction.REDEFINES);
            case OCCURS_DEPENDING_ON_INTENT -> dataIntent(output,
                    DataIntentPayload.Construction.OCCURS_DEPENDING_ON);
            case UNSUPPORTED_EXPLANATION -> new UnsupportedExplanationPayload(
                    requiredText(output, "construction"), requiredText(output, "explanation"),
                    requiredText(output, "manualAction"));
            case DETERMINISTIC -> throw new IllegalArgumentException("deterministic output cannot become a payload");
        };
    }

    private DomainNamingPayload domainNamingPayload(JsonNode output, ResidualAnnotationContext context) {
        DomainNamingPolicy.Decision decision = domainNamingPolicy.validate(
                requiredText(output, "suggestedName"), context.collisionScope(),
                context.publicSignatureProtected());
        return new DomainNamingPayload(decision.normalizedName(), optionalText(output, "boundedContext"),
                requiredText(output, "rationale"));
    }

    private static DataIntentPayload dataIntent(JsonNode output,
                                                DataIntentPayload.Construction expected) {
        String actual = requiredText(output, "construction");
        if (!expected.name().equals(actual)) {
            throw new IllegalArgumentException("data-intent construction does not match the selected route");
        }
        return new DataIntentPayload(expected, requiredText(output, "interpretation"),
                textList(output, "assumptions"));
    }

    private static AnnotationReview review(ResidualRoute route, String reviewer) {
        if (route == ResidualRoute.DOMAIN_NAMING
                || route == ResidualRoute.CONTROL_FLOW_PLAN
                || route == ResidualRoute.REDEFINES_INTENT
                || route == ResidualRoute.OCCURS_DEPENDING_ON_INTENT) {
            if (reviewer == null) throw new IllegalArgumentException("human reviewer is required for this route");
            return new AnnotationReview(AnnotationReview.ReviewState.NEEDS_REVIEW, reviewer, null, null);
        }
        return new AnnotationReview(AnnotationReview.ReviewState.PROPOSED, null, null, null);
    }

    private static String requiredText(JsonNode output, String field) {
        String value = optionalText(output, field);
        if (value == null) throw new IllegalArgumentException(field + " must be a nonblank string");
        return value;
    }

    private static String optionalText(JsonNode output, String field) {
        JsonNode value = output.get(field);
        return value == null || value.isNull() ? null
                : value.isTextual() && !value.textValue().isBlank() ? value.textValue() : null;
    }

    private static List<String> textList(JsonNode output, String field) {
        JsonNode values = output.get(field);
        if (values == null || !values.isArray() || values.isEmpty()) {
            throw new IllegalArgumentException(field + " must be a nonempty array");
        }
        List<String> result = new ArrayList<>();
        values.forEach(value -> {
            if (!value.isTextual() || value.textValue().isBlank()) {
                throw new IllegalArgumentException(field + " entries must be nonblank strings");
            }
            result.add(value.textValue());
        });
        return List.copyOf(result);
    }
}
