package org.shark.renovatio.llm.residual;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.AnnotationProvenance;
import org.shark.renovatio.cobol.ir.annotated.DomainNamingPayload;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainNamingPolicyTest {
    private static final String HASH = "a".repeat(64);
    private static final String NODE = "b".repeat(64);
    private static final ObjectMapper JSON = new ObjectMapper();
    private final DomainNamingPolicy policy = new DomainNamingPolicy();

    @Test
    void normalizesWithTheDeterministicCobolIdentifierConvention() {
        assertEquals("calculateInterest", policy.validate("CALCULATE-INTEREST", List.of(), false).normalizedName());
        assertEquals("customerBalance", DomainNamingPolicy.normalize("customer_balance"));
    }

    @Test
    void rejectsKeywordsAndCaseInsensitiveNormalizedCollisions() {
        assertThrows(IllegalArgumentException.class, () -> policy.validate("class", List.of(), false));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(
                "Calculate-Interest", List.of("calculateInterest"), false));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(
                "customer_balance", List.of("CUSTOMER-BALANCE"), false));
    }

    @Test
    void publicSignatureDecisionIsProtectedAndNeverAutoApplicable() {
        DomainNamingPolicy.Decision decision = policy.validate("calculateInterest", List.of(), true);
        assertTrue(decision.publicSignatureProtected());
        assertFalse(decision.autoApplicable());
    }

    @Test
    void assemblerRejectsCollisionBeforeItCanEnterTheSidecar() {
        AnnotatedCobolModel sidecar = new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                "cobol-ir.v1", HASH, List.of());
        ResidualAnnotationContext context = new ResidualAnnotationContext("cobol-ir.v1", HASH, NODE,
                AnnotatedNodeKind.PARAGRAPH, "offline", "fake", "v1", "domain-naming.v1", HASH,
                "tool-20260830t12345678901234z", AnnotationProvenance.CacheDisposition.MISS, 0.8,
                "project:owner", List.of(), null, List.of("calculateInterest"), false);

        assertThrows(IllegalArgumentException.class, () -> new ResidualAnnotationAssembler().append(sidecar,
                ResidualRoute.DOMAIN_NAMING,
                JSON.createObjectNode().put("suggestedName", "CALCULATE-INTEREST")
                        .put("rationale", "Matches supplied paragraph facts"), context));
        assertTrue(sidecar.annotations().isEmpty());
    }

    @Test
    void assemblerStoresOnlyTheNormalizedReviewableName() {
        AnnotatedCobolModel sidecar = new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                "cobol-ir.v1", HASH, List.of());
        ResidualAnnotationContext context = new ResidualAnnotationContext("cobol-ir.v1", HASH, NODE,
                AnnotatedNodeKind.PARAGRAPH, "offline", "fake", "v1", "domain-naming.v1", HASH,
                "tool-20260830t12345678901234z", AnnotationProvenance.CacheDisposition.MISS, 0.8,
                "project:owner", List.of(), null, List.of("postPayment"), true);

        AnnotatedCobolModel result = new ResidualAnnotationAssembler().append(sidecar,
                ResidualRoute.DOMAIN_NAMING,
                JSON.createObjectNode().put("suggestedName", "CALCULATE-INTEREST")
                        .put("rationale", "Matches supplied paragraph facts"), context);

        assertEquals("calculateInterest",
                ((DomainNamingPayload) result.annotations().get(0).payload()).suggestedName());
        assertFalse(result.annotations().get(0).review().reviewState().name().equals("ACCEPTED"));
    }
}
