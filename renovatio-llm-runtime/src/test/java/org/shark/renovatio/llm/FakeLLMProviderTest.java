package org.shark.renovatio.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.llm.provider.FakeLLMProvider;
import org.shark.renovatio.llm.domain.LLMConfig;
import org.shark.renovatio.llm.domain.ProposalRequest;
import org.shark.renovatio.llm.domain.TypedProposal;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FakeLLMProviderTest {

    private FakeLLMProvider provider;

    @BeforeEach
    void setUp() {
        provider = new FakeLLMProvider();
        provider.configure(LLMConfig.builder().build());
    }

    @Test
    void shouldReturnTemplateForStructureAnalysis() {
        // Arrange
        ProposalRequest request = ProposalRequest.builder()
            .version("1.0")
            .kind("cobol.structure_analysis")
            .input(new ProposalRequest.Input("sha256:abc123", Map.of("programName", "BATCH001")))
            .schema("proposal-request.v1.json")
            .budget(new ProposalRequest.Budget(5000, 5000, 0.50))
            .model(new ProposalRequest.Model("gemini-2.0-flash", "2025-01"))
            .build();

        // Act
        TypedProposal proposal = provider.propose(request);

        // Assert
        assertThat(proposal).isNotNull();
        assertThat(proposal.version()).isEqualTo("1.0");
        assertThat(proposal.kind()).isEqualTo("cobol.structure_analysis");
        assertThat(proposal.confidence()).isEqualTo(0.0);
        assertThat(proposal.rationale()).contains("Fake provider");
        assertThat(proposal.proposal()).containsKey("programName");
        assertThat(proposal.proposal()).containsKey("paragraphs");
        assertThat(proposal.metadata().model()).isEqualTo("fake");
        assertThat(proposal.metadata().cacheStatus()).isEqualTo("BYPASS");
    }

    @Test
    void shouldReturnTemplateForBusinessRule() {
        // Arrange
        ProposalRequest request = ProposalRequest.builder()
            .version("1.0")
            .kind("cobol.business_rule")
            .input(new ProposalRequest.Input("sha256:def456", Map.of()))
            .schema("proposal-request.v1.json")
            .budget(new ProposalRequest.Budget(5000, 5000, 0.50))
            .model(new ProposalRequest.Model("gemini-2.0-flash", "2025-01"))
            .build();

        // Act
        TypedProposal proposal = provider.propose(request);

        // Assert
        assertThat(proposal.kind()).isEqualTo("cobol.business_rule");
        assertThat(proposal.proposal()).containsKey("rules");
    }

    @Test
    void shouldReturnTemplateForDataMapping() {
        // Arrange
        ProposalRequest request = ProposalRequest.builder()
            .version("1.0")
            .kind("cobol.data_mapping")
            .input(new ProposalRequest.Input("sha256:ghi789", Map.of()))
            .schema("proposal-request.v1.json")
            .budget(new ProposalRequest.Budget(5000, 5000, 0.50))
            .model(new ProposalRequest.Model("gemini-2.0-flash", "2025-01"))
            .build();

        // Act
        TypedProposal proposal = provider.propose(request);

        // Assert
        assertThat(proposal.kind()).isEqualTo("cobol.data_mapping");
        assertThat(proposal.proposal()).containsKey("entities");
    }

    @Test
    void shouldReturnCachedTemplateOnSecondCall() {
        // Arrange
        ProposalRequest request = ProposalRequest.builder()
            .version("1.0")
            .kind("cobol.structure_analysis")
            .input(new ProposalRequest.Input("sha256:abc123", Map.of()))
            .schema("proposal-request.v1.json")
            .budget(new ProposalRequest.Budget(5000, 5000, 0.50))
            .model(new ProposalRequest.Model("gemini-2.0-flash", "2025-01"))
            .build();

        // Act
        TypedProposal first = provider.propose(request);
        TypedProposal second = provider.propose(request);

        // Assert - same object from cache
        assertThat(first).isSameAs(second);
    }

    @Test
    void shouldNotShareCachedTemplateAcrossDifferentInputs() {
        ProposalRequest firstRequest = ProposalRequest.builder()
            .version("1.0")
            .kind("cobol.structure_analysis")
            .input(new ProposalRequest.Input("sha256:first", Map.of("programName", "FIRST")))
            .schema("proposal-request.v1.json")
            .budget(new ProposalRequest.Budget(5000, 5000, 0.50))
            .model(new ProposalRequest.Model("gemini-2.0-flash", "2025-01"))
            .build();
        ProposalRequest secondRequest = ProposalRequest.builder()
            .version("1.0")
            .kind("cobol.structure_analysis")
            .input(new ProposalRequest.Input("sha256:second", Map.of("programName", "SECOND")))
            .schema("proposal-request.v1.json")
            .budget(new ProposalRequest.Budget(5000, 5000, 0.50))
            .model(new ProposalRequest.Model("gemini-2.0-flash", "2025-01"))
            .build();

        TypedProposal first = provider.propose(firstRequest);
        TypedProposal second = provider.propose(secondRequest);

        assertThat(first).isNotSameAs(second);
        assertThat(first.proposal()).containsEntry("programName", "FIRST");
        assertThat(second.proposal()).containsEntry("programName", "SECOND");
        assertThat(first.metadata().inputHash()).isEqualTo("sha256:first");
        assertThat(second.metadata().inputHash()).isEqualTo("sha256:second");
    }

    @Test
    void shouldReturnCorrectMetadata() {
        // Act
        var metadata = provider.metadata();

        // Assert
        assertThat(metadata.providerName()).isEqualTo("fake");
        assertThat(metadata.isOffline()).isTrue();
        assertThat(metadata.supportedKinds()).contains(
            "cobol.structure_analysis",
            "cobol.business_rule",
            "cobol.data_mapping"
        );
    }

    @Test
    void shouldBeHealthy() {
        // Assert
        assertThat(provider.isHealthy()).isTrue();
    }
}
