package org.shark.renovatio.provider.cobol.polish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolishContractsTest {

    private static final String HASH = "a".repeat(64);
    private static final String SOURCE = "class Customer { String customerCode; }\n";

    @Test
    void prerequisiteEvidenceRequiresExactSelectorsAndHashes() {
        PolishPrerequisiteEvidence evidence = evidence(true, true, true, true, false);

        assertThat(evidence.isGreen()).isTrue();
        assertThatThrownBy(() -> new PolishPrerequisiteEvidence(
                "abc123", "baseline", List.of(), "mvn test", "17", "3.9.12",
                Map.of("Customer.java", PolishContracts.sha256(SOURCE)),
                Map.of("node-1", PolishContracts.canonicalJsonHash(projection())),
                Map.of("fixture", HASH),
                true, true, true, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("selector");
        assertThatThrownBy(() -> new PolishPrerequisiteEvidence(
                "abc123", "baseline", List.of("fixture"), "mvn test", "17", "3.9.12",
                Map.of("Customer.java", "bad"), Map.of("node-1", HASH), Map.of("fixture", HASH),
                true, true, true, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SHA-256");
    }

    @Test
    void requestBoundaryAllowsOnlyRelativeGeneratedJava() {
        PolishProposalRequest request = request(new DomainNamingRefinement(
                "node-1", "customerCode", "accountCode", Set.of("Customer.java"),
                true, false, false));

        assertThat(request.family()).isEqualTo(PolishProposalFamily.DOMAIN_NAMING_REFINEMENT);
        assertThatThrownBy(() -> new PolishProposalRequest(
                PolishProposalFamily.DOMAIN_NAMING_REFINEMENT, "input.cob", "SAMPLE", HASH,
                "generated", Map.of("../escape.java", "class Escape {}"),
                Map.of("node-1", projection()),
                Map.of("Customer.java", "move-numeric"), Map.of("node-1", "move-numeric"),
                evidence(true, true, true, true, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("generated Java path");
    }

    @Test
    void familyContractsFailClosed() {
        assertThatThrownBy(() -> new DomainNamingRefinement(
                "node-1", "old", "class", Set.of("Customer.java"), true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identifier");
        assertThatThrownBy(() -> new PortExtraction(
                "httpClient", "CustomerPort", Set.of("Customer.java"), true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dependency or configuration");
        assertThatThrownBy(() -> new StrategyExtraction(
                "status-branch", Set.of("ACTIVE", "CLOSED"), false, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exhaustive");
        assertThatThrownBy(() -> new FlagCollapse(
                Set.of("active", "closed"), Set.of("10", "01"),
                Map.of("10", "ACTIVE"), true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("one-to-one");
    }

    @Test
    void allFourClosedProposalFamiliesCanRepresentValidReviewedFacts() {
        assertThat(List.of(
                new DomainNamingRefinement("node-1", "customerCode", "accountCode",
                        Set.of("Customer.java"), true, false, false),
                new PortExtraction("httpClient", "CustomerPort",
                        Set.of("Customer.java"), true, false),
                new StrategyExtraction("node-1", Set.of("ACTIVE", "CLOSED"), true, true),
                new FlagCollapse(Set.of("active", "closed"), Set.of("10", "01"),
                        Map.of("10", "ACTIVE", "01", "CLOSED"), true, true)))
                .extracting(PolishFamilyPayload::family)
                .containsExactly(PolishProposalFamily.values());
    }

    static PolishProposalRequest request(PolishFamilyPayload payload) {
        return new PolishProposalRequest(
                payload.family(), "input.cob", "SAMPLE", HASH, "generated",
                Map.of("Customer.java", SOURCE), Map.of("node-1", projection()),
                Map.of("Customer.java", "move-numeric"), Map.of("node-1", "move-numeric"),
                evidence(true, true, true, true, false));
    }

    static PolishPrerequisiteEvidence evidence(boolean schema, boolean compilation,
                                                boolean characterization, boolean stable,
                                                boolean unresolvedErrors) {
        return new PolishPrerequisiteEvidence(
                "abc123", "baseline-1", List.of("move-numeric"),
                "mvn -B -pl renovatio-provider-cobol -am test", "17", "3.9.12",
                Map.of("Customer.java", PolishContracts.sha256(SOURCE)),
                Map.of("node-1", PolishContracts.canonicalJsonHash(projection())),
                Map.of("move-numeric", HASH),
                schema, compilation, characterization, stable, unresolvedErrors);
    }

    static JsonNode projection() {
        return new ObjectMapper().createObjectNode()
                .put("nodeId", "node-1")
                .put("acceptedDomainTerm", "account");
    }
}
