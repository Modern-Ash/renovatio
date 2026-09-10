package org.shark.renovatio.provider.cobol.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolContext;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolValidator;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.shark.renovatio.cobol.ir.annotated.AnnotationPayload;
import org.shark.renovatio.cobol.ir.annotated.AnnotationProvenance;
import org.shark.renovatio.cobol.ir.annotated.AnnotationReview;
import org.shark.renovatio.cobol.ir.annotated.CobolAnnotation;
import org.shark.renovatio.cobol.ir.annotated.CobolIrIdentityProjector;
import org.shark.renovatio.cobol.ir.annotated.ControlFlowPlanPayload;
import org.shark.renovatio.cobol.ir.annotated.DataIntentPayload;
import org.shark.renovatio.cobol.ir.annotated.DomainNamingPayload;
import org.shark.renovatio.cobol.ir.annotated.UnsupportedExplanationPayload;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.provider.cobol.guardrail.GuardrailSchemaCatalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Resolves and validates an annotated IR sidecar without provider, credential, or network access. */
public final class AnnotatedContextResolver {

    public record Request(Optional<AnnotatedCobolModel> inlineSidecar, Optional<Path> sidecarPath,
                          Path cobolSourcePath) {
        public Request {
            inlineSidecar = inlineSidecar == null ? Optional.empty() : inlineSidecar;
            sidecarPath = sidecarPath == null ? Optional.empty() : sidecarPath;
            Objects.requireNonNull(cobolSourcePath, "cobolSourcePath");
        }
    }

    public record Resolution(Optional<AnnotatedCobolContext> context, List<String> diagnostics) {
        public Resolution {
            context = context == null ? Optional.empty() : context;
            diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
        }
    }

    private final ObjectMapper mapper;
    private final JsonSchema schema;
    private final CobolIrIdentityProjector projector = new CobolIrIdentityProjector();
    private final AnnotatedCobolValidator validator = new AnnotatedCobolValidator();

    public AnnotatedContextResolver(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(new GuardrailSchemaCatalog(mapper).resolve(AnnotatedCobolModel.SCHEMA_VERSION));
    }

    public Resolution resolve(Request request, CobolIntermediateModel model) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(model, "model");
        List<String> diagnostics = new ArrayList<>();

        if (request.inlineSidecar().isPresent()) {
            AnnotatedCobolModel inline = request.inlineSidecar().orElseThrow();
            JsonNode inlineJson = mapper.valueToTree(inline);
            removeNullObjectFields(inlineJson);
            Optional<AnnotatedCobolContext> accepted = accept(inline, inlineJson,
                    "inline", model, diagnostics);
            if (accepted.isPresent()) {
                return new Resolution(accepted, diagnostics);
            }
        }

        Set<Path> candidates = new LinkedHashSet<>();
        request.sidecarPath().map(Path::toAbsolutePath).map(Path::normalize).ifPresent(candidates::add);
        candidates.add(siblingSidecar(request.cobolSourcePath()).toAbsolutePath().normalize());
        for (Path candidate : candidates) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            try {
                JsonNode json = mapper.readTree(candidate.toFile());
                Set<ValidationMessage> schemaErrors = schema.validate(json);
                if (!schemaErrors.isEmpty()) {
                    schemaErrors.stream().map(ValidationMessage::getMessage).sorted()
                            .forEach(error -> diagnostics.add(candidate + ": schema: " + error));
                    continue;
                }
                AnnotatedCobolModel sidecar = decode(json);
                Optional<AnnotatedCobolContext> accepted = accept(sidecar, json, candidate.toString(),
                        model, diagnostics);
                if (accepted.isPresent()) {
                    return new Resolution(accepted, diagnostics);
                }
            } catch (IOException | IllegalArgumentException exception) {
                diagnostics.add(candidate + ": cannot parse annotated sidecar: " + exception.getMessage());
            }
        }
        return new Resolution(Optional.empty(), diagnostics);
    }

    private Optional<AnnotatedCobolContext> accept(AnnotatedCobolModel sidecar, JsonNode json,
                                                   String source, CobolIntermediateModel model,
                                                   List<String> diagnostics) {
        Set<ValidationMessage> schemaErrors = schema.validate(json);
        if (!schemaErrors.isEmpty()) {
            schemaErrors.stream().map(ValidationMessage::getMessage).sorted()
                    .forEach(error -> diagnostics.add(source + ": schema: " + error));
            return Optional.empty();
        }
        String expectedHash = projector.baseIrHash(model);
        if (!expectedHash.equals(sidecar.baseIrHash())) {
            diagnostics.add(source + ": baseIrHash does not match the current COBOL IR");
            return Optional.empty();
        }
        Map<String, AnnotatedNodeKind> nodes = projector.nodes(model).stream()
                .collect(Collectors.toUnmodifiableMap(
                        CobolIrIdentityProjector.ProjectedNode::nodeId,
                        CobolIrIdentityProjector.ProjectedNode::nodeKind));
        List<String> semanticErrors = validator.validate(sidecar, expectedHash, nodes).stream()
                .map(diagnostic -> diagnostic.code() + " " + diagnostic.pointer() + ": " + diagnostic.message())
                .toList();
        if (!semanticErrors.isEmpty()) {
            semanticErrors.forEach(error -> diagnostics.add(source + ": semantic: " + error));
            return Optional.empty();
        }
        return Optional.of(new AnnotatedCobolContext(model, sidecar));
    }

    private AnnotatedCobolModel decode(JsonNode root) throws IOException {
        List<CobolAnnotation> annotations = new ArrayList<>();
        for (JsonNode annotation : root.path("annotations")) {
            AnnotationFamily family = AnnotationFamily.valueOf(annotation.path("annotationFamily").asText());
            JsonNode payloadNode = annotation.path("payload");
            AnnotationPayload payload = switch (family) {
                case DOMAIN_NAMING -> mapper.treeToValue(payloadNode, DomainNamingPayload.class);
                case CONTROL_FLOW_PLAN -> mapper.treeToValue(payloadNode, ControlFlowPlanPayload.class);
                case DATA_INTENT -> mapper.treeToValue(payloadNode, DataIntentPayload.class);
                case UNSUPPORTED_EXPLANATION -> mapper.treeToValue(payloadNode, UnsupportedExplanationPayload.class);
            };
            JsonNode provenanceNode = annotation.path("provenance");
            AnnotationProvenance provenance = new AnnotationProvenance(
                    provenanceNode.path("provider").asText(), provenanceNode.path("model").asText(),
                    provenanceNode.path("promptId").asText(), provenanceNode.path("promptVersion").asText(),
                    provenanceNode.path("outputSchemaVersion").asText(),
                    provenanceNode.path("inputHash").asText(), provenanceNode.path("outputHash").asText(),
                    provenanceNode.path("toolRunRef").asText(),
                    AnnotationProvenance.CacheDisposition.valueOf(
                            provenanceNode.path("cacheDisposition").asText()));
            JsonNode reviewNode = annotation.path("review");
            String reviewedAt = nullableText(reviewNode, "reviewedAt");
            AnnotationReview review = new AnnotationReview(
                    AnnotationReview.ReviewState.valueOf(reviewNode.path("reviewState").asText()),
                    nullableText(reviewNode, "assignedReviewer"), nullableText(reviewNode, "reviewedBy"),
                    reviewedAt == null ? null : Instant.parse(reviewedAt));
            annotations.add(new CobolAnnotation(annotation.path("annotationId").asText(),
                    annotation.path("nodeId").asText(),
                    AnnotatedNodeKind.valueOf(annotation.path("nodeKind").asText()), family, payload,
                    annotation.path("confidence").asDouble(), provenance, review));
        }
        return new AnnotatedCobolModel(root.path("schemaVersion").asText(),
                root.path("baseIrVersion").asText(), root.path("baseIrHash").asText(), annotations);
    }

    private static String nullableText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static void removeNullObjectFields(JsonNode node) {
        if (node.isObject()) {
            List<String> nullFields = new ArrayList<>();
            node.fields().forEachRemaining(entry -> {
                if (entry.getValue().isNull()) {
                    nullFields.add(entry.getKey());
                } else {
                    removeNullObjectFields(entry.getValue());
                }
            });
            nullFields.forEach(((com.fasterxml.jackson.databind.node.ObjectNode) node)::remove);
        } else if (node.isArray()) {
            node.forEach(AnnotatedContextResolver::removeNullObjectFields);
        }
    }

    private static Path siblingSidecar(Path cobolSourcePath) {
        String fileName = cobolSourcePath.getFileName().toString();
        int extension = fileName.lastIndexOf('.');
        String stem = extension > 0 ? fileName.substring(0, extension) : fileName;
        Path parent = cobolSourcePath.toAbsolutePath().normalize().getParent();
        return Objects.requireNonNull(parent, "cobolSourcePath parent")
                .resolve(stem + ".annotated.json");
    }
}
