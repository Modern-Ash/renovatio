package org.shark.renovatio.llm.adapter.cobol;

import org.shark.renovatio.llm.domain.LLMProvider;
import org.shark.renovatio.llm.domain.ProposalMetadata;
import org.shark.renovatio.llm.domain.ProposalRequest;
import org.shark.renovatio.llm.domain.TypedProposal;
import org.shark.renovatio.llm.domain.LLMConfig;
import org.shark.renovatio.llm.security.PromptSanitizer;
import org.shark.renovatio.llm.security.DataMinimizer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * COBOL-specific LLM adapter.
 * Translates COBOL analysis requests to ProposalRequest and processes TypedProposal responses.
 * This adapter lives in the llm-runtime module to keep COBOL core free of LLM dependencies.
 */
@Component
public class CobolLLMAdapter {

    private final LLMProvider llmProvider;
    private final PromptSanitizer promptSanitizer;
    private final DataMinimizer dataMinimizer;

    public CobolLLMAdapter(LLMProvider llmProvider, PromptSanitizer promptSanitizer, DataMinimizer dataMinimizer) {
        this.llmProvider = llmProvider;
        this.promptSanitizer = promptSanitizer;
        this.dataMinimizer = dataMinimizer;
    }

    /**
     * Analyzes COBOL program structure using LLM.
     */
    public TypedProposal analyzeStructure(String programName, String sourceHash, Map<String, Object> context) {
        Map<String, Object> sanitizedContext = promptSanitizer.sanitizeContext(context);
        Map<String, Object> minimizedContext = dataMinimizer.minimize(sanitizedContext);

        ProposalRequest request = ProposalRequest.builder()
            .version("1.0")
            .kind("cobol.structure_analysis")
            .input(new ProposalRequest.Input(sourceHash, minimizedContext))
            .schema("proposal-request.v1.json")
            .budget(new ProposalRequest.Budget(5000, 5000, 0.50))
            .model(new ProposalRequest.Model("gemini-2.0-flash", "2025-01"))
            .build();

        return llmProvider.propose(request);
    }

    /**
     * Extracts business rules from COBOL code using LLM.
     */
    public TypedProposal extractBusinessRules(String programName, String sourceHash, Map<String, Object> context) {
        Map<String, Object> sanitizedContext = promptSanitizer.sanitizeContext(context);
        Map<String, Object> minimizedContext = dataMinimizer.minimize(sanitizedContext);

        ProposalRequest request = ProposalRequest.builder()
            .version("1.0")
            .kind("cobol.business_rule")
            .input(new ProposalRequest.Input(sourceHash, minimizedContext))
            .schema("proposal-request.v1.json")
            .budget(new ProposalRequest.Budget(5000, 5000, 0.50))
            .model(new ProposalRequest.Model("gemini-2.0-flash", "2025-01"))
            .build();

        return llmProvider.propose(request);
    }

    /**
     * Maps COBOL copybook data structures to JPA entities using LLM.
     */
    public TypedProposal mapDataStructures(String programName, String sourceHash, Map<String, Object> context) {
        Map<String, Object> sanitizedContext = promptSanitizer.sanitizeContext(context);
        Map<String, Object> minimizedContext = dataMinimizer.minimize(sanitizedContext);

        ProposalRequest request = ProposalRequest.builder()
            .version("1.0")
            .kind("cobol.data_mapping")
            .input(new ProposalRequest.Input(sourceHash, minimizedContext))
            .schema("proposal-request.v1.json")
            .budget(new ProposalRequest.Budget(5000, 5000, 0.50))
            .model(new ProposalRequest.Model("gemini-2.0-flash", "2025-01"))
            .build();

        return llmProvider.propose(request);
    }

    public ProposalMetadata metadata() {
        return llmProvider.metadata();
    }

    public void configure(LLMConfig config) {
        llmProvider.configure(config);
    }

    public boolean isHealthy() {
        return llmProvider.isHealthy();
    }
}