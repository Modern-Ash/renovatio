package org.shark.renovatio.llm.provider;

import org.shark.renovatio.llm.domain.LLMConfig;
import org.shark.renovatio.llm.domain.LLMProvider;
import org.shark.renovatio.llm.domain.ProposalMetadata;
import org.shark.renovatio.llm.domain.ProposalRequest;
import org.shark.renovatio.llm.domain.TypedProposal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Offline fake LLM provider for development and testing.
 * Returns template-based responses without network calls.
 * Activated by default when no remote provider is configured.
 */
public class FakeLLMProvider implements LLMProvider {

    private final Map<String, TypedProposal> templateCache = new ConcurrentHashMap<>();
    private LLMConfig config;

    @Override
    public TypedProposal propose(ProposalRequest request) {
        String templateKey = request.kind() + "#" + request.schema() + "#" + request.input().sourceHash();

        return templateCache.computeIfAbsent(templateKey, k -> generateTemplate(request));
    }

    @Override
    public ProposalMetadata metadata() {
        return new ProposalMetadata(
            "fake",
            "1.0.0",
            "fake",
            "offline",
            true,
            List.of(
                "cobol.structure_analysis",
                "cobol.business_rule",
                "cobol.data_mapping"
            )
        );
    }

    @Override
    public void configure(LLMConfig config) {
        this.config = config;
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    private TypedProposal generateTemplate(ProposalRequest request) {
        Map<String, Object> proposalContent = switch (request.kind()) {
            case "cobol.structure_analysis" -> Map.of(
                "programName", request.input().context().getOrDefault("programName", "UNKNOWN"),
                "paragraphs", List.of("INITIALIZE", "PROCESS", "FINALIZE"),
                "dataItems", List.of("WS-COUNTER", "WS-TOTAL"),
                "controlFlow", List.of("SEQUENTIAL")
            );
            case "cobol.business_rule" -> Map.of(
                "rules", List.of(
                    Map.of("name", "RULE-001", "description", "Business rule extracted from COBOL"),
                    Map.of("name", "RULE-002", "description", "Validation rule for input data")
                )
            );
            case "cobol.data_mapping" -> Map.of(
                "entities", List.of(
                    Map.of("name", "Entity", "fields", List.of("id", "name", "value"))
                )
            );
            default -> Map.of("error", "Unknown kind: " + request.kind());
        };

        return TypedProposal.builder()
            .version("1.0")
            .kind(request.kind())
            .proposal(proposalContent)
            .confidence(0.0)
            .rationale("Fake provider - offline mode, template response")
            .schema(request.schema())
            .metadata(new TypedProposal.Metadata(
                "fake",
                "sha256:fake-prompt",
                request.input().sourceHash(),
                "sha256:fake-output",
                0,
                0,
                "BYPASS"
            ))
            .build();
    }
}
