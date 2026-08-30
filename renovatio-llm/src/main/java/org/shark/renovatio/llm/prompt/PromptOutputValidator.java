package org.shark.renovatio.llm.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import org.shark.renovatio.llm.enrichment.PersistenceSanitizer;
import org.shark.renovatio.llm.provider.ProviderFailure;

/** Executes the catalog's ordered, closed validator vocabulary. */
public final class PromptOutputValidator {
    private final PromptRuntime runtime;
    private final StrictJsonSchemaValidator schemaValidator;
    private final PersistenceSanitizer sanitizer;

    public PromptOutputValidator(PromptRuntime runtime, PersistenceSanitizer sanitizer) {
        this.runtime = runtime;
        this.schemaValidator = new StrictJsonSchemaValidator();
        this.sanitizer = sanitizer;
    }

    public JsonNode validate(PreparedEnrichment prepared, JsonNode output) {
        for (String validator : prepared.definition().validators()) {
            switch (validator) {
                case "json-schema.v1" -> schemaValidator.validate(output,
                        runtime.resource(prepared.definition().outputSchema()));
                case "annotated-ir-reference.v1" -> validateReferences(output,
                        prepared.identity().canonicalInput());
                case "public-signature-preservation.v1" -> rejectSignatureMutation(output);
                case "sanitized-persistence.v1" -> sanitizer.sanitize(output);
                case "deterministic-fallback.v1" -> { /* Applied only by CatalogFallbackFactory. */ }
                default -> throw new OutputValidationException(ProviderFailure.VALIDATOR_REJECTED);
            }
        }
        return sanitizer.sanitize(output);
    }

    private static void validateReferences(JsonNode output, JsonNode input) {
        output.fields().forEachRemaining(field -> {
            if (field.getKey().endsWith("NodeId") && field.getValue().isTextual()
                    && !input.toString().contains(field.getValue().textValue())) {
                throw new OutputValidationException(ProviderFailure.VALIDATOR_REJECTED);
            }
        });
    }

    private static void rejectSignatureMutation(JsonNode output) {
        if (output.has("publicSignature") || output.has("signatureChange")) {
            throw new OutputValidationException(ProviderFailure.VALIDATOR_REJECTED);
        }
    }
}
