package org.shark.renovatio.provider.cobol.guardrail;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    void rejectsUnknownSchemaVersion() {
        assertThatThrownBy(() -> catalog.resolve("manual-action-item.v2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported guardrail schema version");
    }
}
