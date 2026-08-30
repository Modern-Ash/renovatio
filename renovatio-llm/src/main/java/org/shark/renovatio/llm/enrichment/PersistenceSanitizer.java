package org.shark.renovatio.llm.enrichment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Iterator;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.regex.Pattern;

/** Deterministic allowlist/redaction boundary for committed cache content. */
public final class PersistenceSanitizer {
    private static final String ALLOWLIST_RESOURCE = "schemas/persistence-allowlist.v1.json";
    private static final Pattern SECRET = Pattern.compile(
            "(?i)(bearer\\s+[a-z0-9._-]+|sk-[a-z0-9_-]+|api[_-]?key\\s*[:=])");
    private final Set<String> allowedFields;
    private final Set<String> forbiddenFields;
    private final int maximumStringLength;

    public PersistenceSanitizer() {
        Allowlist allowlist = loadAllowlist();
        this.allowedFields = Set.copyOf(allowlist.allowedFields());
        this.forbiddenFields = Set.copyOf(allowlist.forbiddenFields());
        this.maximumStringLength = allowlist.maximumStringLength();
        if (!"persistence-allowlist.v1".equals(allowlist.schemaVersion())
                || allowedFields.isEmpty() || maximumStringLength < 1) {
            throw new IllegalStateException("LLM_PERSISTENCE_ALLOWLIST_INVALID");
        }
    }

    public JsonNode sanitize(JsonNode value) {
        if (value == null || (!value.isObject() && !value.isArray())) {
            throw new SanitizationException();
        }
        validate(value, true);
        return value.deepCopy();
    }

    private void validate(JsonNode node, boolean enforceLlmAllowlist) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (forbiddenFields.contains(field.getKey())
                        || (enforceLlmAllowlist && !allowedFields.contains(field.getKey()))) {
                    throw new SanitizationException();
                }
                // deterministicResult is produced by the deterministic transpiler, not by the LLM.
                // It has an open domain schema but still shares the secret, size and JSON-type boundary.
                validate(field.getValue(), enforceLlmAllowlist
                        && !"deterministicResult".equals(field.getKey()));
            }
        } else if (node.isArray()) {
            for (JsonNode element : (ArrayNode) node) validate(element, enforceLlmAllowlist);
        } else if (node.isTextual()) {
            String text = node.textValue();
            if (text.length() > maximumStringLength || SECRET.matcher(text).find()) {
                throw new SanitizationException();
            }
        } else if (!node.isNumber() && !node.isBoolean() && !node.isNull()) {
            throw new SanitizationException();
        }
    }

    private static Allowlist loadAllowlist() {
        try (InputStream input = PersistenceSanitizer.class.getClassLoader()
                .getResourceAsStream(ALLOWLIST_RESOURCE)) {
            if (input == null) throw new IllegalStateException("LLM_PERSISTENCE_ALLOWLIST_MISSING");
            return new ObjectMapper().readValue(input, Allowlist.class);
        } catch (IOException exception) {
            throw new IllegalStateException("LLM_PERSISTENCE_ALLOWLIST_INVALID", exception);
        }
    }

    private record Allowlist(String schemaVersion, Set<String> allowedFields,
                             Set<String> forbiddenFields, int maximumStringLength) { }

    public static final class SanitizationException extends RuntimeException {
        public SanitizationException() {
            super("LLM_SANITIZATION_REJECTED");
        }
    }
}
