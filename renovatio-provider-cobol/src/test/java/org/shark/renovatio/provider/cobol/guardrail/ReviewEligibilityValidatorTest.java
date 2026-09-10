package org.shark.renovatio.provider.cobol.guardrail;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewEligibilityValidatorTest {

    private static final String HASH = "a".repeat(64);

    @Test
    void admitsOnlyBoundedReproducibleAttributedAndApprovedChanges() {
        GateCheckResult result = new ReviewEligibilityValidator().validate(request(
                Set.of("generated/Sample.java"),
                Set.of("org.example.Sample#run()"),
                Set.of("org.example.Sample#run()"),
                Map.of("baseIr", HASH),
                true));

        assertThat(result.passed()).isTrue();
        assertThat(result.diagnosticReference()).isEqualTo("review:eligible");
    }

    @Test
    void failsClosedForEveryReviewEligibilityCondition() {
        ReviewEligibilityValidator validator = new ReviewEligibilityValidator();

        assertThat(validator.validate(request(Set.of("generated/Sample.java"), Set.of(), Set.of(),
                Map.of("baseIr", HASH), false)).diagnosticReference())
                .isEqualTo("review:not-byte-reproducible");
        assertThat(validator.validate(request(Set.of("other.java"), Set.of(), Set.of(),
                Map.of("baseIr", HASH), true)).diagnosticReference())
                .isEqualTo("review:undeclared-path");
        assertThat(validator.validate(requestWithManifest(
                new ProposalManifest(Set.of("generated/Sample.java"), Map.of("input.cob", HASH), Map.of()),
                Set.of("generated/Sample.java"), Set.of(), Set.of(), Map.of("baseIr", HASH), true))
                .diagnosticReference())
                .isEqualTo("review:missing-output-hash");
        assertThat(validator.validate(request(Set.of("generated/Sample.java"), Set.of(), Set.of(),
                Map.of(), true)).diagnosticReference())
                .isEqualTo("review:missing-provenance");
        assertThat(validator.validate(request(Set.of("generated/Sample.java"),
                Set.of("org.example.Sample#run()"), Set.of(), Map.of("baseIr", HASH), true))
                .diagnosticReference())
                .isEqualTo("review:unapproved-public-signature");
    }

    @Test
    void rejectsUnboundedOrNonContentAddressedManifests() {
        assertThatThrownBy(() -> new ProposalManifest(Set.of("../outside"), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProposalManifest(Set.of("generated/Sample.java"),
                Map.of(), Map.of("generated/Sample.java", "not-a-hash")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProposalManifest(Set.of("generated/Sample.java"),
                Map.of(), Map.of("other.java", HASH)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ReviewEligibilityRequest request(
            Set<String> changedPaths,
            Set<String> signatureChanges,
            Set<String> approvals,
            Map<String, String> provenance,
            boolean reproducible) {
        ProposalManifest manifest = new ProposalManifest(
                Set.of("generated/Sample.java"),
                Map.of("input.cob", HASH),
                Map.of("generated/Sample.java", HASH));
        return requestWithManifest(manifest, changedPaths, signatureChanges, approvals, provenance, reproducible);
    }

    private static ReviewEligibilityRequest requestWithManifest(
            ProposalManifest manifest,
            Set<String> changedPaths,
            Set<String> signatureChanges,
            Set<String> approvals,
            Map<String, String> provenance,
            boolean reproducible) {
        return new ReviewEligibilityRequest(
                manifest, changedPaths, signatureChanges, approvals, provenance, reproducible);
    }
}
