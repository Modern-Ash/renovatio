package org.shark.renovatio.provider.cobol.guardrail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

/** Resolves only explicitly supported guardrail schema versions. */
public final class GuardrailSchemaCatalog {

    private static final Map<String, String> RESOURCES = Map.of(
            "cobol-ir.v1", "/schema/cobol-ir.v1.schema.json",
            "cobol-annotated-ir.v1", "/schema/cobol-annotated-ir.v1.schema.json",
            "manual-action-item.v1", "/schema/manual-action-item.v1.schema.json",
            "idiomatic-polish-proposal.v1", "/schema/idiomatic-polish-proposal.v1.schema.json");

    private final ObjectMapper objectMapper;

    public GuardrailSchemaCatalog(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public JsonNode resolve(String version) {
        String resource = RESOURCES.get(version);
        if (resource == null) {
            throw new IllegalArgumentException("Unsupported guardrail schema version: " + version);
        }
        try (InputStream input = GuardrailSchemaCatalog.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Guardrail schema resource is missing: " + resource);
            }
            return objectMapper.readTree(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read guardrail schema: " + resource, exception);
        }
    }
}
