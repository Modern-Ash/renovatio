package org.shark.renovatio.llm.prompt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PromptCatalogLoaderTest {

    private final PromptCatalogLoader loader = new PromptCatalogLoader();

    @Test
    void loadsAllVersionedEntriesInCatalogOrder() {
        PromptCatalog catalog = loader.loadDefault();

        assertEquals(List.of(
                        "cobol.domain.naming.v1",
                        "cobol.goto.restructure.v1",
                        "cobol.redefines.intent.v1",
                        "cobol.occurs-depending.intent.v1",
                        "cobol.unsupported.explain.v1",
                        "cobol.naming.suggest.v1",
                        "cobol.documentation.generate.v1",
                        "cobol.move-corresponding.intent.v1",
                        "decision.numeric.v1",
                        "decision.control-flow.v1",
                        "decision.data-shape.v1",
                        "decision.persistence.v1",
                        "decision.naming.v1",
                        "decision.architecture.v1"),
                catalog.entries().stream().map(PromptDefinition::promptId).toList());
        assertEquals("DOMAIN_NAMING", catalog.require("cobol.domain.naming.v1").appliesTo());
    }

    @Test
    void rejectsUnknownPromptAndUnknownYamlFields() {
        assertThrows(IllegalArgumentException.class, () -> loader.loadDefault().require("missing.v1"));
        assertCode("PROMPT_CATALOG_INVALID_YAML", validEntry() + "unknown: true\n");
    }

    @Test
    void rejectsUnversionedIdEmptyExamplesAndUnknownSelector() {
        assertCode("PROMPT_ID_UNVERSIONED", validEntry().replace("sample.prompt.v1", "sample.prompt"));
        assertCode("PROMPT_FEW_SHOT_EMPTY", validEntry().replace(
                "fewShot:\n  - input: {name: A}\n    output: {name: B}", "fewShot: []"));
        assertCode("PROMPT_SELECTOR_UNKNOWN", validEntry().replace("DOMAIN_NAMING", "UNKNOWN"));
        assertCode("PROMPT_SYSTEM_EMPTY", validEntry().replace("system: Name the node.", "system: ''"));
        assertCode("PROMPT_FEW_SHOT_INVALID", validEntry().replace("input: {name: A}", "input:"));
    }

    @Test
    void rejectsUnknownValidatorAndMissingResources() {
        assertCode("PROMPT_VALIDATOR_UNKNOWN", validEntry().replace("json-schema.v1", "unknown.v1"));
        assertCode("PROMPT_VALIDATOR_UNKNOWN", validEntry().replace("[json-schema.v1]", "[]"));
        assertCode("PROMPT_SCHEMA_UNVERSIONED", validEntry().replace("schema.v1.schema.json", "schema.json"));
        assertCode("PROMPT_SCHEMA_MISSING", validEntry().replace("schema.v1.schema.json", "missing.v1.schema.json"));
        assertCode("PROMPT_FALLBACK_MISSING", validEntry().replace("fallback.yaml", "missing.yaml"));
    }

    @Test
    void rejectsDuplicatePromptIds() {
        PromptCatalogException exception = assertThrows(PromptCatalogException.class,
                () -> loader.loadEntries(List.of(validEntry(), validEntry()), this::resourceExists));
        assertEquals("PROMPT_ID_DUPLICATE", exception.code());
    }

    @Test
    void rejectsMalformedUnversionedAndEmptyFallbackContracts() {
        assertFallbackCode("PROMPT_FALLBACK_VERSION_INVALID", validFallback()
                .replace("renovatio-llm-fallback.v1", "legacy"));
        assertFallbackCode("PROMPT_FALLBACK_TYPE_INVALID", validFallback()
                .replace("MANUAL_ACTION", "MODEL_OUTPUT"));
        assertFallbackCode("PROMPT_FALLBACK_ACTION_EMPTY", validFallback()
                .replace("Review manually.", "''"));
        assertFallbackCode("PROMPT_FALLBACK_DIAGNOSTIC_INVALID", validFallback()
                .replace("LLM_MANUAL_REVIEW_REQUIRED", "manual-review"));
        assertFallbackCode("PROMPT_FALLBACK_INVALID", validFallback() + "unknown: true\n");
    }

    private void assertFallbackCode(String code, String yaml) {
        PromptCatalogException exception = assertThrows(PromptCatalogException.class,
                () -> loader.validateFallbackResource(yaml));
        assertEquals(code, exception.code());
    }

    private void assertCode(String code, String yaml) {
        PromptCatalogException exception = assertThrows(PromptCatalogException.class,
                () -> loader.loadEntry(yaml, this::resourceExists));
        assertEquals(code, exception.code());
    }

    private boolean resourceExists(String resource) {
        return resource.equals("schema.v1.schema.json") || resource.equals("fallback.yaml");
    }

    private static String validEntry() {
        return """
                promptId: sample.prompt.v1
                appliesTo: DOMAIN_NAMING
                system: Name the node.
                fewShot:
                  - input: {name: A}
                    output: {name: B}
                outputSchema: schema.v1.schema.json
                validators: [json-schema.v1]
                fallback: fallback.yaml
                """;
    }

    private static String validFallback() {
        return """
                fallbackVersion: renovatio-llm-fallback.v1
                type: MANUAL_ACTION
                diagnosticCode: LLM_MANUAL_REVIEW_REQUIRED
                manualAction: Review manually.
                """;
    }
}
