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

/** Strict loader for the immutable v1 prompt catalog. */
public final class PromptCatalogLoader {
    public static final String INDEX_RESOURCE = "prompts/catalog-v1.yaml";

    private static final Pattern VERSIONED_ID = Pattern.compile("[a-z0-9]+(?:[.-][a-z0-9]+)*\\.v[1-9][0-9]*");
    private static final Set<String> SELECTORS = Set.of(
            "DOMAIN_NAMING", "CONTROL_FLOW_PLAN", "DATA_INTENT.REDEFINES",
            "DATA_INTENT.OCCURS_DEPENDING_ON", "UNSUPPORTED_EXPLANATION");
    private static final Set<String> VALIDATORS = Set.of(
            "json-schema.v1", "annotated-ir-reference.v1", "public-signature-preservation.v1",
            "deterministic-fallback.v1", "sanitized-persistence.v1");

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
            return validate(entries, resource -> classLoader.getResource(resource) != null);
        } catch (PromptCatalogException exception) {
            throw exception;
        } catch (IOException exception) {
            throw error("PROMPT_CATALOG_INVALID_YAML", "Cannot load prompt catalog");
        }
    }

    PromptCatalog loadEntry(String content, Predicate<String> resourceExists) {
        try {
            return validate(List.of(yaml.readValue(content, PromptDefinition.class)), resourceExists);
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
            require(entry.fewShot().stream().allMatch(example -> example.input() != null && example.output() != null),
                    "PROMPT_FEW_SHOT_INVALID", "fewShot examples require input and output");
            require(entry.validators() != null && !entry.validators().isEmpty()
                            && entry.validators().stream().allMatch(VALIDATORS::contains),
                    "PROMPT_VALIDATOR_UNKNOWN", "Unknown or empty validators");
            require(entry.outputSchema() != null && resourceExists.test(entry.outputSchema()),
                    "PROMPT_SCHEMA_MISSING", "Output schema resource is missing");
            require(entry.fallback() != null && resourceExists.test(entry.fallback()),
                    "PROMPT_FALLBACK_MISSING", "Fallback resource is missing");
        }
        return new PromptCatalog(entries);
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
