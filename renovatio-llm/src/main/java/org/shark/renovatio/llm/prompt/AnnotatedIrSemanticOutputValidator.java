package org.shark.renovatio.llm.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolValidator;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedIdentity;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.shark.renovatio.cobol.ir.annotated.AnnotationPayload;
import org.shark.renovatio.cobol.ir.annotated.AnnotationProvenance;
import org.shark.renovatio.cobol.ir.annotated.AnnotationReview;
import org.shark.renovatio.cobol.ir.annotated.CobolAnnotation;
import org.shark.renovatio.cobol.ir.annotated.ControlFlowPlanPayload;
import org.shark.renovatio.cobol.ir.annotated.DataIntentPayload;
import org.shark.renovatio.cobol.ir.annotated.DomainNamingPayload;
import org.shark.renovatio.cobol.ir.annotated.UnsupportedExplanationPayload;
import org.shark.renovatio.cobol.ir.annotated.CanonicalJson;
import org.shark.renovatio.llm.cache.CacheKey;
import org.shark.renovatio.llm.provider.ProviderFailure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Projects a validated prompt payload through the accepted annotated-IR semantic contract. */
final class AnnotatedIrSemanticOutputValidator {
    void validate(PreparedEnrichment prepared, JsonNode output) {
        try {
            JsonNode input = prepared.identity().canonicalInput();
            String appliesTo = prepared.definition().appliesTo();
            AnnotationFamily family = family(appliesTo);
            // Route-discriminator guard: the shared data-intent.v1 schema accepts any construction,
            // so a MOVE_CORRESPONDING prompt answering `construction: REDEFINES` would otherwise be
            // cached as MODEL_SUCCESS and only rejected later by the assembler. Pin it here.
            if (family == AnnotationFamily.DATA_INTENT && appliesTo.startsWith("DATA_INTENT.")
                    && !appliesTo.substring("DATA_INTENT.".length())
                            .equals(output.path("construction").asText())) {
                throw new OutputValidationException(ProviderFailure.VALIDATOR_REJECTED);
            }
            AnnotatedNodeKind defaultKind = switch (family) {
                case DATA_INTENT -> AnnotatedNodeKind.DATA_ITEM;
                default -> AnnotatedNodeKind.PARAGRAPH;
            };
            String nodeId = hashOrDerived(input.path("nodeId").asText(), input);
            AnnotatedNodeKind nodeKind = input.hasNonNull("nodeKind")
                    ? AnnotatedNodeKind.valueOf(input.path("nodeKind").asText()) : defaultKind;
            String baseIrHash = hashOrDerived(input.path("baseIrHash").asText(), input);
            String baseIrVersion = input.path("baseIrVersion").asText("cobol-ir.v1");
            AnnotationPayload payload = payload(family, output, nodeId);
            double confidence = 0.0d;
            String inputHash = CacheKey.sha256(CanonicalJson.write(
                    new com.fasterxml.jackson.databind.ObjectMapper().convertValue(input, Object.class)));
            String outputHash = AnnotatedIdentity.outputHash(family, payload, confidence);
            AnnotationProvenance provenance = new AnnotationProvenance(
                    prepared.identity().provider(), prepared.identity().model(),
                    prepared.identity().promptId(), promptVersion(prepared.identity().promptId()),
                    prepared.identity().outputSchemaId(), inputHash, outputHash,
                    "tool-19700101t00000000000000z", AnnotationProvenance.CacheDisposition.MISS);
            CobolAnnotation annotation = new CobolAnnotation(
                    AnnotatedIdentity.annotationId(nodeId, family, provenance), nodeId, nodeKind,
                    family, payload, confidence, provenance,
                    new AnnotationReview(AnnotationReview.ReviewState.PROPOSED, null, null, null));
            AnnotatedCobolModel sidecar = new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                    baseIrVersion, baseIrHash, List.of(annotation));
            Map<String, AnnotatedNodeKind> nodes = nodes(input, nodeId, nodeKind);
            if (!new AnnotatedCobolValidator().validate(sidecar, baseIrHash, nodes).isEmpty()) {
                throw new OutputValidationException(ProviderFailure.VALIDATOR_REJECTED);
            }
        } catch (OutputValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new OutputValidationException(ProviderFailure.VALIDATOR_REJECTED);
        }
    }

    private static AnnotationFamily family(String appliesTo) {
        return switch (appliesTo) {
            case "DOMAIN_NAMING" -> AnnotationFamily.DOMAIN_NAMING;
            case "CONTROL_FLOW_PLAN" -> AnnotationFamily.CONTROL_FLOW_PLAN;
            case "DATA_INTENT.REDEFINES", "DATA_INTENT.OCCURS_DEPENDING_ON", "DATA_INTENT.MOVE_CORRESPONDING" -> AnnotationFamily.DATA_INTENT;
            case "UNSUPPORTED_EXPLANATION" -> AnnotationFamily.UNSUPPORTED_EXPLANATION;
            default -> throw new IllegalArgumentException("Unsupported annotation family");
        };
    }

    private static AnnotationPayload payload(AnnotationFamily family, JsonNode output, String nodeId) {
        return switch (family) {
            case DOMAIN_NAMING -> new DomainNamingPayload(output.path("suggestedName").asText(),
                    output.hasNonNull("boundedContext") ? output.path("boundedContext").asText() : null,
                    output.path("rationale").asText());
            case CONTROL_FLOW_PLAN -> new ControlFlowPlanPayload(List.of(nodeId),
                    strings(output.path("steps")), strings(output.path("risks")));
            case DATA_INTENT -> new DataIntentPayload(
                    DataIntentPayload.Construction.valueOf(output.path("construction").asText()),
                    output.path("interpretation").asText(), strings(output.path("assumptions")));
            case UNSUPPORTED_EXPLANATION -> new UnsupportedExplanationPayload(
                    output.path("construction").asText(), output.path("explanation").asText(),
                    output.path("manualAction").asText());
        };
    }

    private static List<String> strings(JsonNode array) {
        if (!array.isArray()) throw new IllegalArgumentException("array");
        return java.util.stream.StreamSupport.stream(array.spliterator(), false)
                .map(JsonNode::asText).toList();
    }

    private static Map<String, AnnotatedNodeKind> nodes(JsonNode input, String nodeId,
                                                        AnnotatedNodeKind nodeKind) {
        Map<String, AnnotatedNodeKind> nodes = new LinkedHashMap<>();
        JsonNode declared = input.path("semanticNodes");
        if (declared.isObject()) declared.fields().forEachRemaining(item ->
                nodes.put(item.getKey(), AnnotatedNodeKind.valueOf(item.getValue().asText())));
        nodes.putIfAbsent(nodeId, nodeKind);
        return Map.copyOf(nodes);
    }

    private static String hashOrDerived(String candidate, JsonNode fallback) {
        return candidate.matches("[0-9a-f]{64}") ? candidate : CacheKey.sha256(
                candidate.isBlank() ? fallback.toString() : candidate);
    }

    private static String promptVersion(String promptId) {
        int marker = promptId.lastIndexOf(".v");
        return marker < 0 ? "v1" : promptId.substring(marker + 1);
    }
}
