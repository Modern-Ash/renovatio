package org.shark.renovatio.provider.cobol.guardrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

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
        assertThat(catalog.resolve("cobol-annotated-ir.v1").path("$id").asText())
                .isEqualTo("https://renovatio.dev/schema/cobol-annotated-ir.v1.schema.json");
    }

    @Test
    void annotatedIrSchemaIsClosedAndDiscriminatesAllPayloadFamilies() {
        JsonNode schema = catalog.resolve("cobol-annotated-ir.v1");

        assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
        assertThat(schema.path("properties").path("annotations").path("minItems").asInt()).isEqualTo(1);
        assertThat(schema.path("$defs").path("annotation").path("oneOf").size()).isEqualTo(4);
        assertThat(schema.path("$defs").path("provenance").path("additionalProperties").asBoolean()).isFalse();
        assertThat(schema.path("$defs").path("review").path("oneOf").size()).isEqualTo(4);
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

    @Test
    void validatesCommittedAnnotatedIrFixtures() throws IOException {
        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(catalog.resolve("cobol-annotated-ir.v1"));

        assertThat(validateFixture(schema, "/fixtures/annotated-ir/valid-domain-naming.annotated.json")).isEmpty();
        assertThat(validateFixture(schema, "/fixtures/annotated-ir/invalid-unknown-property.annotated.json")).isNotEmpty();
        assertThat(validateFixture(schema, "/fixtures/annotated-ir/invalid-review-state.annotated.json")).isNotEmpty();
        assertThat(validateFixture(schema, "/fixtures/annotated-ir/invalid-family-payload.annotated.json")).isNotEmpty();
    }

    private Set<ValidationMessage> validateFixture(JsonSchema schema, String resource) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            assertThat(input).as("fixture %s", resource).isNotNull();
            return schema.validate(new ObjectMapper().readTree(input));
        }
    }
}
