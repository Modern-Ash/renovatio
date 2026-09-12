package org.shark.renovatio.llm.governance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.llm.domain.LLMConfig;
import org.shark.renovatio.llm.domain.ProposalRequest;
import org.shark.renovatio.llm.domain.TypedProposal;
import org.shark.renovatio.llm.security.GovernanceRedactor;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceLoggerTest {

    private GovernanceLogger logger;
    private GovernanceRedactor redactor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        redactor = new GovernanceRedactor();
        logger = new GovernanceLogger(redactor);
    }

    @Test
    void shouldLogCallAndReturnDecisionId() {
        // Arrange
        ProposalRequest request = ProposalRequest.builder()
            .version("1.0")
            .kind("cobol.structure_analysis")
            .input(new ProposalRequest.Input("sha256:abc123", Map.of("programName", "BATCH001")))
            .schema("proposal-request.v1.json")
            .budget(new ProposalRequest.Budget(5000, 5000, 0.50))
            .model(new ProposalRequest.Model("gemini-2.0-flash", "2025-01"))
            .build();

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

        LLMConfig config = LLMConfig.builder()
            .provider("gemini")
            .model("gemini-2.0-flash")
            .build();

        // Act
        String decisionId = logger.logCall("test-actor", request, proposal, config, 1234, "MISS");

        // Assert
        assertThat(decisionId).isNotNull();
        assertThat(decisionId).matches("[0-9a-f-]{36}");
    }

    @Test
    void shouldRecordDecision() {
        // Arrange
        ProposalRequest request = ProposalRequest.builder()
            .version("1.0")
            .kind("cobol.structure_analysis")
            .input(new ProposalRequest.Input("sha256:abc123", Map.of()))
            .schema("proposal-request.v1.json")
            .budget(new ProposalRequest.Budget(5000, 5000, 0.50))
            .model(new ProposalRequest.Model("gemini-2.0-flash", "2025-01"))
            .build();

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

        LLMConfig config = LLMConfig.builder().build();

        // Act
        String decisionId = logger.logCall("test-actor", request, proposal, config, 1234, "MISS");

        // Record acceptance
        logger.recordDecision(decisionId, "spec-owner", GovernanceLogger.Decision.ACCEPT, "Approved for production");

        // Assert
        var entry = logger.getPendingDecision(decisionId);
        assertThat(entry).isNull(); // Should be removed after decision
    }

    @Test
    void shouldRecordRejection() {
        // Arrange
        ProposalRequest request = ProposalRequest.builder()
            .version("1.0")
            .kind("cobol.structure_analysis")
            .input(new ProposalRequest.Input("sha256:abc123", Map.of()))
            .schema("proposal-request.v1.json")
            .budget(new ProposalRequest.Budget(5000, 5000, 0.50))
            .model(new ProposalRequest.Model("gemini-2.0-flash", "2025-01"))
            .build();

        TypedProposal proposal = TypedProposal.builder()
            .version("1.0")
            .kind("cobol.structure_analysis")
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

        LLMConfig config = LLMConfig.builder().build();

        // Act
        String decisionId = logger.logCall("test-actor", request, proposal, config, 1234, "MISS");

        // Record rejection
        logger.recordDecision(decisionId, "spec-owner", GovernanceLogger.Decision.REJECT, "Confidence too low");

        // Assert
        var entry = logger.getPendingDecision(decisionId);
        assertThat(entry).isNull(); // Should be removed after decision
    }

    @Test
    void shouldRecordFallback() {
        // Arrange
        ProposalRequest request = ProposalRequest.builder()
            .version("1.0")
            .kind("cobol.structure_analysis")
            .input(new ProposalRequest.Input("sha256:abc123", Map.of()))
            .schema("proposal-request.v1.json")
            .budget(new ProposalRequest.Budget(5000, 5000, 0.50))
            .model(new ProposalRequest.Model("gemini-2.0-flash", "2025-01"))
            .build();

        TypedProposal proposal = TypedProposal.builder()
            .version("1.0")
            .kind("cobol.structure_analysis")
            .proposal(Map.of())
            .confidence(0.50)
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

        LLMConfig config = LLMConfig.builder().build();

        // Act
        String decisionId = logger.logCall("test-actor", request, proposal, config, 1234, "MISS");

        // Record fallback
        logger.recordDecision(decisionId, "spec-owner", GovernanceLogger.Decision.FALLBACK, "Provider timeout, using fake");

        // Assert
        var entry = logger.getPendingDecision(decisionId);
        assertThat(entry).isNull(); // Should be removed after decision
    }
}