package org.shark.renovatio.llm.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.llm.provider.ProviderFailure;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Deterministic validator for the closed JSON Schema subset used by catalog v1. */
public final class StrictJsonSchemaValidator {
    private static final ObjectMapper JSON = new ObjectMapper();

    public void validate(JsonNode value, byte[] schemaBytes) {
        try {
            JsonNode schema = JSON.readTree(schemaBytes);
            validateNode(value, schema);
        } catch (IOException exception) {
            throw new OutputValidationException(ProviderFailure.OUTPUT_SCHEMA_INVALID);
        }
    }

    private void validateNode(JsonNode value, JsonNode schema) {
        String type = schema.path("type").asText();
        if ("object".equals(type)) validateObject(value, schema);
        else if ("array".equals(type)) validateArray(value, schema);
        else if ("string".equals(type)) validateString(value, schema);
        if (schema.has("enum")) {
            boolean found = false;
            for (JsonNode allowed : schema.path("enum")) found |= allowed.equals(value);
            require(found);
        }
    }

    private void validateObject(JsonNode value, JsonNode schema) {
        require(value != null && value.isObject());
        Set<String> properties = new HashSet<>();
        schema.path("properties").fieldNames().forEachRemaining(properties::add);
        for (JsonNode required : schema.path("required")) require(value.has(required.asText()));
        if (!schema.path("additionalProperties").asBoolean(true)) {
            Iterator<String> names = value.fieldNames();
            while (names.hasNext()) require(properties.contains(names.next()));
        }
        for (String property : properties) {
            if (value.has(property)) validateNode(value.get(property), schema.path("properties").path(property));
        }
    }

    private void validateArray(JsonNode value, JsonNode schema) {
        require(value != null && value.isArray());
        require(value.size() >= schema.path("minItems").asInt(0));
        for (JsonNode item : value) validateNode(item, schema.path("items"));
    }

    private void validateString(JsonNode value, JsonNode schema) {
        require(value != null && value.isTextual());
        require(value.textValue().length() >= schema.path("minLength").asInt(0));
    }

    private static void require(boolean condition) {
        if (!condition) throw new OutputValidationException(ProviderFailure.OUTPUT_SCHEMA_INVALID);
    }
}
