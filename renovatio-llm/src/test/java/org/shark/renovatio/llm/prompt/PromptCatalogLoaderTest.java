package org.shark.renovatio.llm.prompt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PromptCatalogLoaderTest {

    private final PromptCatalogLoader loader = new PromptCatalogLoader();

    @Test
    void loadsTheFiveVersionedEntriesInCatalogOrder() {
        PromptCatalog catalog = loader.loadDefault();

        assertEquals(List.of(
                        "cobol.domain.naming.v1",
                        "cobol.goto.restructure.v1",
                        "cobol.redefines.intent.v1",
                        "cobol.occurs-depending.intent.v1",
                        "cobol.unsupported.explain.v1"),
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
    }

    @Test
    void rejectsUnknownValidatorAndMissingResources() {
        assertCode("PROMPT_VALIDATOR_UNKNOWN", validEntry().replace("json-schema.v1", "unknown.v1"));
        assertCode("PROMPT_SCHEMA_MISSING", validEntry().replace("schema.json", "missing.json"));
        assertCode("PROMPT_FALLBACK_MISSING", validEntry().replace("fallback.yaml", "missing.yaml"));
    }

    private void assertCode(String code, String yaml) {
        PromptCatalogException exception = assertThrows(PromptCatalogException.class,
                () -> loader.loadEntry(yaml, resource -> resource.equals("schema.json")
                        || resource.equals("fallback.yaml")));
        assertEquals(code, exception.code());
    }

    private static String validEntry() {
        return """
                promptId: sample.prompt.v1
                appliesTo: DOMAIN_NAMING
                system: Name the node.
                fewShot:
                  - input: {name: A}
                    output: {name: B}
                outputSchema: schema.json
                validators: [json-schema.v1]
                fallback: fallback.yaml
                """;
    }
}
