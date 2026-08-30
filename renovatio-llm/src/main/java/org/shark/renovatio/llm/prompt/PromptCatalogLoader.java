package org.shark.renovatio.llm.prompt;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;

/** Strict loader for the immutable v1 prompt catalog. */
public final class PromptCatalogLoader {
    public static final String INDEX_RESOURCE = "prompts/catalog-v1.yaml";

    private static final Pattern VERSIONED_ID = Pattern.compile("[a-z0-9]+(?:[.-][a-z0-9]+)*\\.v[1-9][0-9]*");
    private static final Pattern VERSIONED_SCHEMA = Pattern.compile(
            "(?:[a-z0-9.-]+/)*[a-z0-9]+(?:[.-][a-z0-9]+)*\\.v[1-9][0-9]*\\.schema\\.json");
    private static final Set<String> SELECTORS = Set.of(
            "DOMAIN_NAMING", "CONTROL_FLOW_PLAN", "DATA_INTENT.REDEFINES",
            "DATA_INTENT.OCCURS_DEPENDING_ON", "UNSUPPORTED_EXPLANATION");
    private static final Set<String> VALIDATORS = Set.of(
            "json-schema.v1", "annotated-ir-reference.v1", "public-signature-preservation.v1",
            "sanitized-persistence.v1");

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public PromptCatalog loadDefault() {
        ClassLoader classLoader = PromptCatalogLoader.class.getClassLoader();
        try (InputStream input = required(classLoader, INDEX_RESOURCE)) {
            CatalogIndex index = yaml.readValue(input, CatalogIndex.class);
            if (index.prompts() == null || index.prompts().isEmpty()) {
                throw error("PROMPT_CATALOG_EMPTY", "Catalog index must contain prompt resources");
            }
            List<PromptDefinition> entries = new ArrayList<>();
            for (String resource : index.prompts()) {
                try (InputStream promptInput = required(classLoader, resource)) {
                    entries.add(yaml.readValue(promptInput, PromptDefinition.class));
                }
            }
            PromptCatalog catalog = validate(entries, resource -> classLoader.getResource(resource) != null);
            for (PromptDefinition entry : entries) {
                try (InputStream fallback = required(classLoader, entry.fallback())) {
                    validateFallback(entry, yaml.readTree(fallback));
                }
            }
            return catalog;
        } catch (PromptCatalogException exception) {
            throw exception;
        } catch (IOException exception) {
            throw error("PROMPT_CATALOG_INVALID_YAML", "Cannot load prompt catalog");
        }
    }

    PromptCatalog loadEntry(String content, Predicate<String> resourceExists) {
        return loadEntries(List.of(content), resourceExists);
    }

    PromptCatalog loadEntries(List<String> contents, Predicate<String> resourceExists) {
        try {
            List<PromptDefinition> entries = new ArrayList<>();
            for (String content : contents) {
                entries.add(yaml.readValue(content, PromptDefinition.class));
            }
            return validate(entries, resourceExists);
        } catch (PromptCatalogException exception) {
            throw exception;
        } catch (IOException exception) {
            throw error("PROMPT_CATALOG_INVALID_YAML", "Cannot parse prompt entry");
        }
    }

    private PromptCatalog validate(List<PromptDefinition> entries, Predicate<String> resourceExists) {
        Set<String> ids = new HashSet<>();
        for (PromptDefinition entry : entries) {
            require(entry.promptId() != null && VERSIONED_ID.matcher(entry.promptId()).matches(),
                    "PROMPT_ID_UNVERSIONED", "promptId must have a vN suffix");
            require(ids.add(entry.promptId()), "PROMPT_ID_DUPLICATE", "Duplicate promptId");
            require(SELECTORS.contains(entry.appliesTo()), "PROMPT_SELECTOR_UNKNOWN", "Unknown appliesTo");
            require(entry.system() != null && !entry.system().isBlank(), "PROMPT_SYSTEM_EMPTY", "system is required");
            require(entry.fewShot() != null && !entry.fewShot().isEmpty(),
                    "PROMPT_FEW_SHOT_EMPTY", "fewShot must not be empty");
            require(entry.fewShot().stream().allMatch(example -> example.input() != null
                            && !example.input().isNull() && example.output() != null && !example.output().isNull()),
                    "PROMPT_FEW_SHOT_INVALID", "fewShot examples require input and output");
            require(entry.validators() != null && !entry.validators().isEmpty()
                            && entry.validators().stream().allMatch(VALIDATORS::contains),
                    "PROMPT_VALIDATOR_UNKNOWN", "Unknown or empty validators");
            require(entry.outputSchema() != null && VERSIONED_SCHEMA.matcher(entry.outputSchema()).matches(),
                    "PROMPT_SCHEMA_UNVERSIONED", "Output schema resource must have a vN.schema.json suffix");
            require(resourceExists.test(entry.outputSchema()),
                    "PROMPT_SCHEMA_MISSING", "Output schema resource is missing");
            require(entry.fallback() != null && resourceExists.test(entry.fallback()),
                    "PROMPT_FALLBACK_MISSING", "Fallback resource is missing");
        }
        return new PromptCatalog(entries);
    }

    private void validateFallback(PromptDefinition entry, JsonNode fallback) {
        require(fallback.isObject() && fallback.size() == 4,
                "PROMPT_FALLBACK_INVALID", "Fallback must use the strict v1 contract");
        require("renovatio-llm-fallback.v1".equals(fallback.path("fallbackVersion").asText()),
                "PROMPT_FALLBACK_VERSION_INVALID", "Fallback version is invalid");
        require("MANUAL_ACTION".equals(fallback.path("type").asText()),
                "PROMPT_FALLBACK_TYPE_INVALID", "Fallback type is invalid");
        require(fallback.path("diagnosticCode").asText().matches("LLM_[A-Z0-9_]+"),
                "PROMPT_FALLBACK_DIAGNOSTIC_INVALID", "Fallback diagnostic code is invalid");
        require(!fallback.path("manualAction").asText().isBlank(),
                "PROMPT_FALLBACK_ACTION_EMPTY", "Fallback manual action is required");
    }

    void validateFallbackResource(String content) {
        try {
            validateFallback(null, yaml.readTree(content));
        } catch (IOException exception) {
            throw error("PROMPT_FALLBACK_INVALID", "Fallback YAML is invalid");
        }
    }

    private static InputStream required(ClassLoader loader, String resource) {
        InputStream input = loader.getResourceAsStream(resource);
        if (input == null) {
            throw error("PROMPT_RESOURCE_MISSING", "Missing catalog resource: " + resource);
        }
        return input;
    }

    private static void require(boolean condition, String code, String message) {
        if (!condition) {
            throw error(code, message);
        }
    }

    private static PromptCatalogException error(String code, String message) {
        return new PromptCatalogException(code, message);
    }

    private record CatalogIndex(List<String> prompts) {
    }
}
