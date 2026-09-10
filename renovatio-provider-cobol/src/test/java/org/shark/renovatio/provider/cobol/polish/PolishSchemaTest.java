package org.shark.renovatio.provider.cobol.polish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.provider.cobol.guardrail.GuardrailGate;
import org.shark.renovatio.provider.cobol.guardrail.GuardrailSchemaCatalog;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolishSchemaTest {

    private static final String HASH = "a".repeat(64);

    @Test
    void proposalSchemaIsClosedAndDiscriminatesFourFamilies() {
        JsonNode schema = new GuardrailSchemaCatalog(new ObjectMapper())
                .resolve("idiomatic-polish-proposal.v1");

        assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
        assertThat(schema.path("$defs").path("familyPayload").path("oneOf")).hasSize(4);
    }

    @Test
    void rejectsUnknownManifestProperties() {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(new GuardrailSchemaCatalog(mapper).resolve("idiomatic-polish-proposal.v1"));
        JsonNode invalid = mapper.createObjectNode()
                .put("schemaVersion", "idiomatic-polish-proposal.v1")
                .put("unknown", true);

        assertThat(schema.validate(invalid)).isNotEmpty();
    }

    @Test
    void acceptsTheClosedReviewOnlyManifestShape() {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(new GuardrailSchemaCatalog(mapper).resolve("idiomatic-polish-proposal.v1"));
        PolishProposalManifest manifest = new PolishProposalManifest(
                PolishProposalManifest.SCHEMA_VERSION, "polish-" + "a".repeat(24),
                PolishProposalFamily.DOMAIN_NAMING_REFINEMENT, "PROPOSED",
                PolishDisposition.ELIGIBLE_FOR_REVIEW, "abc123", "baseline-1",
                List.of("move-numeric"), "mvn test", "17", "3.9.12",
                Map.of("Customer.java", HASH), Map.of("node-1", HASH),
                Map.of("move-numeric", HASH), Map.of("Customer.java", "move-numeric"),
                Map.of("node-1", "move-numeric"), Map.of("Customer.java", HASH), HASH,
                Set.of("Customer.java"), HASH,
                Map.of("promptId", PolishProposalFamily.DOMAIN_NAMING_REFINEMENT.promptId()),
                Set.of(), List.of(GuardrailGate.values()), "all-gates-passed",
                mapper.valueToTree(new DomainNamingRefinement(
                        "node-1", "customerCode", "accountCode", Set.of("Customer.java"),
                        true, false, false)));

        var messages = schema.validate(mapper.valueToTree(manifest));
        assertThat(messages).describedAs(messages.toString()).isEmpty();
    }
}
