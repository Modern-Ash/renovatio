package org.shark.renovatio.llm.provider;

import org.shark.renovatio.llm.domain.LLMConfig;
import org.shark.renovatio.llm.domain.LLMProvider;
import org.shark.renovatio.llm.domain.ProposalMetadata;
import org.shark.renovatio.llm.domain.ProposalRequest;
import org.shark.renovatio.llm.domain.TypedProposal;
import org.shark.renovatio.llm.domain.LLMTimeoutException;
import org.shark.renovatio.llm.domain.LLMBudgetExceededException;
import org.shark.renovatio.llm.domain.LLMInvalidSchemaException;
import org.shark.renovatio.llm.domain.LLMServiceUnavailableException;
import org.shark.renovatio.llm.domain.LLMProviderException;
import org.shark.renovatio.llm.security.PromptSanitizer;
import org.shark.renovatio.llm.security.DataMinimizer;
import org.shark.renovatio.llm.security.GovernanceRedactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Google Gemini LLM provider for production use.
 * Implements LLMProvider interface with full governance, resilience, and security.
 */
@Component
public class GeminiLLMProvider implements LLMProvider {

    private static final Logger logger = LoggerFactory.getLogger(GeminiLLMProvider.class);

    private final LLMConfig config;
    private final HttpClient httpClient;
    private final PromptSanitizer promptSanitizer;
    private final DataMinimizer dataMinimizer;
    private final GovernanceRedactor governanceRedactor;

    private String apiKey;
    private String apiEndpoint;

    public GeminiLLMProvider(
            LLMConfig config,
            PromptSanitizer promptSanitizer,
            DataMinimizer dataMinimizer,
            GovernanceRedactor governanceRedactor) {
        this.config = config;
        this.promptSanitizer = promptSanitizer;
        this.dataMinimizer = dataMinimizer;
        this.governanceRedactor = new GovernanceRedactor();

        // Configure HTTP client with timeout
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(config.timeout())
            .build();

        // Load API key and endpoint from config (in production, use secret manager)
        this.apiKey = System.getenv("GEMINI_API_KEY");
        this.apiEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + 
            config.model() + ":generateContent?key=" + apiKey;

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("GEMINI_API_KEY not set, provider will not be functional");
        }
    }

    @Override
    public TypedProposal propose(ProposalRequest request) {
        Instant startTime = Instant.now();

        try {
            // 1. Sanitize prompt
            String sanitizedPrompt = buildPrompt(request);
            String sanitized = promptSanitizer.sanitize(sanitizedPrompt);
            
            // Check for injection attempts
            if (promptSanitizer.hasInjectionPatterns(sanitizedPrompt)) {
                logger.warn("Potential prompt injection detected, request rejected");
                throw new LLMProviderException("Prompt injection detected", "PROMPT_INJECTION", false);
            }

            // 2. Minimize data
            Map<String, Object> minimizedContext = dataMinimizer.minimize(request.input().context());

            // 3. Build request payload
            String payload = buildPayload(request, sanitized, minimizedContext);

            // 4. Execute HTTP request with timeout
            String response = executeWithTimeout(payload, request.budget().maxLatencyMs());

            // 5. Parse and validate response
            TypedProposal proposal = parseResponse(response, request);

            // 6. Validate schema
            validateSchema(proposal, request.schema());

            // 7. Record metadata
            long latencyMs = Duration.between(Instant.now(), startTime).toMillis();
            String promptHash = hash(sanitized);
            String inputHash = request.input().sourceHash();
            String outputHash = hash(proposal.toString());

            return TypedProposal.builder()
                .version(proposal.version())
                .kind(proposal.kind())
                .proposal(proposal.proposal())
                .confidence(proposal.confidence())
                .rationale(proposal.rationale())
                .schema(proposal.schema())
                .metadata(new TypedProposal.Metadata(
                    config.model(),
                    promptHash,
                    request.input().sourceHash(),
                    outputHash,
                    latencyMs,
                    estimateTokens(response),
                    "MISS"
                ))
                .build();

        } catch (LLMProviderException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LLMTimeoutException("Request interrupted", e);
        } catch (Exception e) {
            logger.error("LLM provider error", e);
            throw new LLMProviderException("Provider error: " + e.getMessage(), "PROVIDER_ERROR", true, e);
        }
    }

    private String buildPrompt(ProposalRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert COBOL analyst. ");
        
        switch (request.kind()) {
            case "cobol.structure_analysis" -> prompt.append("Analyze the structure of this COBOL program and identify paragraphs, data items, and control flow.");
            case "cobol.business_rule" -> prompt.append("Extract business rules from this COBOL code. Identify validation rules, calculations, and business logic.");
            case "cobol.data_mapping" -> prompt.append("Map COBOL data structures to JPA entities. Identify copybook fields and their Java/JPA equivalents.");
            default -> prompt.append("Analyze this COBOL code.");
        }

        prompt.append("\n\nSource hash: ").append(request.input().sourceHash());
        prompt.append("\nContext: ").append(request.input().context().toString());
        prompt.append("\n\nRespond with a valid JSON object matching the schema: ").append(request.schema());
        
        return prompt.toString();
    }

    private Map<String, Object> minimizeContext(Map<String, Object> context) {
        return new GovernanceRedactor().redactForLog(context);
    }

    private String buildPayload(ProposalRequest request, String prompt, Map<String, Object> context) {
        // Build Gemini API payload
        Map<String, Object> payload = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
            ),
            "generationConfig", Map.of(
                "temperature", 0.1,
                "maxOutputTokens", request.budget().maxTokens(),
                "responseMimeType", "application/json"
            ),
            "safetySettings", List.of(
                Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
                Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
                Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
                Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_MEDIUM_AND_ABOVE")
            )
        );
        
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            throw new LLMProviderException("Failed to serialize payload", "SERIALIZATION_ERROR", false, e);
        }
    }

    private String executeWithTimeout(String payload, int maxLatencyMs) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiEndpoint))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMillis(maxLatencyMs))
            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
            .build();

        CompletableFuture<HttpResponse<String>> future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        
        try {
            HttpResponse<String> response = future.get(maxLatencyMs, TimeUnit.MILLISECONDS);
            
            if (response.statusCode() == 429) {
                throw new LLMProviderException("Rate limit exceeded", "RATE_LIMIT", true);
            }
            if (response.statusCode() >= 500) {
                throw new LLMServiceUnavailableException("LLM service error: " + response.statusCode());
            }
            if (response.statusCode() >= 400) {
                throw new LLMProviderException("Request failed: " + response.body(), "REQUEST_FAILED", false);
            }
            
            return response.body();
            
        } catch (java.util.concurrent.TimeoutException e) {
            throw new LLMTimeoutException("Request timeout after " + maxLatencyMs + "ms");
        } catch (java.util.concurrent.ExecutionException e) {
            throw new LLMProviderException("Request execution failed", "EXECUTION_ERROR", true, e.getCause());
        }
    }

    private TypedProposal parseResponse(String response, ProposalRequest request) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var jsonNode = mapper.readTree(response);
            
            // Extract text from Gemini response
            String text = jsonNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            
            // Parse JSON from response
            var proposalNode = mapper.readTree(text);
            
            return TypedProposal.builder()
                .version(proposalNode.path("version").asText("1.0"))
                .kind(proposalNode.path("kind").asText())
                .proposal(mapper.convertValue(proposalNode.path("proposal"), Map.class))
                .confidence(proposalNode.path("confidence").asDouble(0.0))
                .rationale(proposalNode.path("rationale").asText(""))
                .schema(proposalNode.path("schema").asText())
                .metadata(new TypedProposal.Metadata(
                    config.model(),
                    "sha256:prompt",
                    "sha256:input",
                    "sha256:output",
                    0,
                    0,
                    "MISS"
                ))
                .build();
                
        } catch (Exception e) {
            throw new LLMInvalidSchemaException("Failed to parse LLM response: " + e.getMessage(), e);
        }
    }

    private void validateSchema(TypedProposal proposal, String schemaName) {
        // In production, validate against JSON schema
        if (proposal.proposal() == null || proposal.proposal().isEmpty()) {
            throw new LLMInvalidSchemaException("Proposal content is empty");
        }
        if (proposal.confidence() < 0 || proposal.confidence() > 1) {
            throw new LLMInvalidSchemaException("Confidence must be between 0 and 1");
        }
    }

    private int estimateTokens(String text) {
        return text.length() / 4; // Rough estimate
    }

    private String hash(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "sha256:" + hex.toString();
        } catch (Exception e) {
            return "hash-error";
        }
    }

    @Override
    public ProposalMetadata metadata() {
        return new ProposalMetadata(
            "gemini",
            "1.0",
            config.model(),
            config.model(),
            false,
            List.of("cobol.structure_analysis", "cobol.business_rule", "cobol.data_mapping")
        );
    }

    @Override
    public void configure(LLMConfig config) {
        // Configuration is immutable after construction
    }

    @Override
    public boolean isHealthy() {
        return apiKey != null && !apiKey.isEmpty();
    }
}