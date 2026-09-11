package org.shark.renovatio.llm.governance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.llm.domain.TypedProposal;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecisionSetTest {

    private DecisionSet decisionSet;

    @BeforeEach
    void setUp() {
        decisionSet = new DecisionSet();
    }

    @Test
    void shouldSubmitForReview() {
        // Arrange
        TypedProposal proposal = TypedProposal.builder()
            .version("1.0")
            .kind("cobol.structure_analysis")
            .proposal(Map.of("programName", "BATCH001"))
            .confidence(0.92)
            .rationale("Structure analysis complete")
            .schema("typed-proposal.v1.json")
            .metadata(new TypedProposal.Metadata(
                "gemini-2.0-flash",
                "sha256:prompt",
                "sha256:abc123",
                "sha256:output",
                1234,
                847,
                "MISS"
            ))
            .build();

        // Act
        String decisionId = decisionSet.submitForReview(
            "dec-123",
            "cobol.structure_analysis",
            proposal,
            "test-actor",
            "High confidence proposal"
        );

        // Assert
        assertThat(decisionId).isEqualTo("dec-123");
        assertThat(decisionSet.isPending("dec-123")).isTrue();
    }

    @Test
    void shouldAcceptProposal() {
        // Arrange
        TypedProposal proposal = TypedProposal.builder()
            .version("1.0")
            .kind("cobol.structure_analysis")
            .proposal(Map.of())
            .confidence(0.92)
            .rationale("Test")
            .schema("typed-proposal.v1.json")
            .metadata(new TypedProposal.Metadata(
                "gemini-2.0-flash",
                "sha256:prompt",
                "sha256:abc123",
                "sha256:output",
                1234,
                847,
                "MISS"
            ))
            .build();

        String decisionId = decisionSet.submitForReview("dec-123", "test", proposal, "actor", "test");

        // Act
        decisionSet.accept("dec-123", "spec-owner", "Approved");

        // Assert
        var record = decisionSet.getDecision("dec-123");
        assertThat(record).isNotNull();
        assertThat(record.getDecision()).isEqualTo(GovernanceLogger.Decision.ACCEPT);
        assertThat(record.getDecidedBy()).isEqualTo("spec-owner");
        assertThat(record.getDecisionRationale()).isEqualTo("Approved");
        assertThat(decisionSet.isPending("dec-123")).isFalse();
    }

    @Test
    void shouldRejectProposal() {
        // Arrange
        TypedProposal proposal = TypedProposal.builder()
            .version("1.0")
            .kind("cobol.business_rule")
            .proposal(Map.of())
            .confidence(0.50)
            .rationale("Low confidence")
            .schema("typed-proposal.v1.json")
            .metadata(new TypedProposal.Metadata(
                "gemini-2.0-flash",
                "sha256:prompt",
                "sha256:abc123",
                "sha256:output",
                1234,
                847,
                "MISS"
            ))
            .build();

        String decisionId = decisionSet.submitForReview("dec-123", "test", proposal, "actor", "test");

        // Act
        decisionSet.reject("dec-123", "spec-owner", "Confidence below threshold");

        // Assert
        var record = decisionSet.getDecision("dec-123");
        assertThat(record).isNotNull();
        assertThat(record.getDecision()).isEqualTo(GovernanceLogger.Decision.REJECT);
        assertThat(record.getDecidedBy()).isEqualTo("spec-owner");
        assertThat(record.getDecisionRationale()).isEqualTo("Confidence below threshold");
        assertThat(decisionSet.isPending("dec-123")).isFalse();
    }

    @Test
    void shouldFallbackProposal() {
        // Arrange
        TypedProposal proposal = TypedProposal.builder()
            .version("1.0")
            .kind("cobol.data_mapping")
            .proposal(Map.of())
            .confidence(0.0)
            .rationale("Provider unavailable")
            .schema("typed-proposal.v1.json")
            .metadata(new TypedProposal.Metadata(
                "gemini-2.0-flash",
                "sha256:prompt",
                "sha256:abc123",
                "sha256:output",
                1234,
                847,
                "MISS"
            ))
            .build();

        String decisionId = decisionSet.submitForReview("dec-123", "test", proposal, "actor", "test");

        // Act
        decisionSet.fallback("dec-123", "spec-owner", "Provider timeout");

        // Assert
        var record = decisionSet.getDecision("dec-123");
        assertThat(record).isNotNull();
        assertThat(record.getDecision()).isEqualTo(GovernanceLogger.Decision.FALLBACK);
        assertThat(record.getDecidedBy()).isEqualTo("spec-owner");
        assertThat(record.getDecisionRationale()).isEqualTo("Provider timeout");
        assertThat(decisionSet.isPending("dec-123")).isFalse();
    }

    @Test
    void shouldThrowOnAcceptNonExistent() {
        assertThatThrownBy(() -> decisionSet.accept("non-existent", "actor", "test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No pending decision");
    }

    @Test
    void shouldThrowOnRejectNonExistent() {
        assertThatThrownBy(() -> decisionSet.reject("non-existent", "actor", "test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No pending decision");
    }

    @Test
    void shouldGetPendingDecisions() {
        // Arrange
        TypedProposal proposal = TypedProposal.builder()
            .version("1.0")
            .kind("test")
            .proposal(Map.of())
            .confidence(0.92)
            .rationale("Test")
            .schema("typed-proposal.v1.json")
            .metadata(new TypedProposal.Metadata(
                "gemini-2.0-flash",
                "sha256:prompt",
                "sha256:abc123",
                "sha256:output",
                1234,
                847,
                "MISS"
            ))
            .build();

        decisionSet.submitForReview("dec-1", "test", proposal, "actor1", "test");
        decisionSet.submitForReview("dec-2", "test", proposal, "actor2", "test");
        decisionSet.accept("dec-1", "spec-owner", "Accepted");

        // Act
        var allPending = decisionSet.getPendingDecisions(null);
        var actor1Pending = decisionSet.getPendingDecisions("actor1");
        var actor2Pending = decisionSet.getPendingDecisions("actor2");

        // Assert
        assertThat(allPending).hasSize(1);
        assertThat(actor1Pending).isEmpty();
        assertThat(actor2Pending).hasSize(1);
    }
}