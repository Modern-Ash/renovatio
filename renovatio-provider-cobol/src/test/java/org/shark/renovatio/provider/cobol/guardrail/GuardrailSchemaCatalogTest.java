package org.shark.renovatio.provider.cobol.guardrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardrailSchemaCatalogTest {

    private final GuardrailSchemaCatalog catalog = new GuardrailSchemaCatalog(new ObjectMapper());

    @Test
    void resolvesCommittedSchema() {
        assertThat(catalog.resolve("manual-action-item.v1").path("$id").asText())
                .isEqualTo("https://renovatio.dev/schema/manual-action-item.v1.schema.json");
        assertThat(catalog.resolve("cobol-ir.v1").path("$id").asText())
                .isEqualTo("https://renovatio.dev/schema/cobol-ir.v1.schema.json");
    }

    @Test
    void constrainsParagraphAndDataItemCollections() {
        JsonNode definitions = catalog.resolve("cobol-ir.v1").path("$defs");

        assertThat(definitions.isObject()).isTrue();
        assertThat(definitions.has("paragraph")).isTrue();
        assertThat(definitions.has("dataItem")).isTrue();
        assertThat(definitions.path("paragraph").path("additionalProperties").asBoolean()).isFalse();
        assertThat(definitions.path("dataItem").path("additionalProperties").asBoolean()).isFalse();
        assertThat(definitions.path("statement").path("oneOf").size()).isEqualTo(8);
        assertThat(catalog.resolve("cobol-ir.v1").path("properties").path("paragraphs")
                .path("additionalProperties").path("$ref").asText()).isEqualTo("#/$defs/paragraph");
        assertThat(catalog.resolve("cobol-ir.v1").path("properties").path("dataItems")
                .path("items").path("$ref").asText()).isEqualTo("#/$defs/dataItem");
    }

    @Test
    void rejectsUnknownSchemaVersion() {
        assertThatThrownBy(() -> catalog.resolve("manual-action-item.v2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported guardrail schema version");
    }
}
