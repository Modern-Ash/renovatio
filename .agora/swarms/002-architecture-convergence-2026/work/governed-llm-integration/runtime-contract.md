# Runtime Contract: LLM Provider Governado

## ProposalRequest v1.0

### Schema JSON
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "https://renovatio.shark.org/schemas/proposal-request.v1.json",
  "title": "ProposalRequest",
  "type": "object",
  "required": ["version", "kind", "input", "schema", "budget", "model"],
  "properties": {
    "version": { "type": "string", "const": "1.0" },
    "kind": {
      "type": "string",
      "enum": ["cobol.structure_analysis", "cobol.business_rule", "cobol.data_mapping"]
    },
    "input": {
      "type": "object",
      "required": ["sourceHash", "context"],
      "properties": {
        "sourceHash": { "type": "string", "pattern": "^sha256:[a-f0-9]{64}$" },
        "context": { "type": "object" }
      }
    },
    "schema": { "type": "string", "pattern": "^[a-z-]+\\.v\\d+\\.json$" },
    "budget": {
      "type": "object",
      "required": ["maxTokens", "maxLatencyMs"],
      "properties": {
        "maxTokens": { "type": "integer", "minimum": 100, "maximum": 50000 },
        "maxLatencyMs": { "type": "integer", "minimum": 100, "maximum": 30000 },
        "maxCostUSD": { "type": "number", "minimum": 0, "maximum": 10 }
      }
    },
    "model": {
      "type": "object",
      "required": ["name", "version"],
      "properties": {
        "name": { "type": "string" },
        "version": { "type": "string" }
      }
    }
  }
}
```

## TypedProposal v1.0

### Schema JSON
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "https://renovatio.shark.org/schemas/typed-proposal.v1.json",
  "title": "TypedProposal",
  "type": "object",
  "required": ["version", "kind", "proposal", "confidence", "rationale", "schema", "metadata"],
  "properties": {
    "version": { "type": "string", "const": "1.0" },
    "kind": {
      "type": "string",
      "enum": ["cobol.structure_analysis", "cobol.business_rule", "cobol.data_mapping"]
    },
    "proposal": { "type": "object" },
    "confidence": { "type": "number", "minimum": 0, "maximum": 1 },
    "rationale": { "type": "string", "maxLength": 5000 },
    "schema": { "type": "string", "pattern": "^[a-z-]+\\.v\\d+\\.json$" },
    "metadata": {
      "type": "object",
      "required": ["model", "promptHash", "inputHash", "outputHash", "latencyMs", "tokensUsed", "cacheStatus"],
      "properties": {
        "model": { "type": "string" },
        "promptHash": { "type": "string", "pattern": "^sha256:[a-f0-9]{64}$" },
        "inputHash": { "type": "string", "pattern": "^sha256:[a-f0-9]{64}$" },
        "outputHash": { "type": "string", "pattern": "^sha256:[a-f0-9]{64}$" },
        "latencyMs": { "type": "integer", "minimum": 0 },
        "tokensUsed": { "type": "integer", "minimum": 0 },
        "cacheStatus": { "type": "string", "enum": ["HIT", "MISS", "BYPASS"] }
      }
    }
  }
}
```

## LLMProvider Interface (Java)

```java
package org.shark.renovatio.llm.provider;

import org.shark.renovatio.llm.domain.ProposalRequest;
import org.shark.renovatio.llm.domain.TypedProposal;
import org.shark.renovatio.llm.domain.LLMConfig;

public interface LLMProvider {

    /**
     * Genera una propuesta tipada para el request dado.
     * @throws LLMProviderException si falla el proveedor
     * @throws LLMTimeoutException si excede timeout configurado
     * @throws LLMBudgetExceededException si excede presupuesto
     */
    TypedProposal propose(ProposalRequest request);

    /**
     * Metadata del proveedor para gobernanza.
     */
    ProposalMetadata metadata();

    /**
     * Configura el proveedor en runtime.
     */
    void configure(LLMConfig config);

    /**
     * Health check del proveedor.
     */
    boolean isHealthy();
}
```

## LLMConfig Record

```java
package org.shark.renovatio.llm.domain;

import java.time.Duration;

public record LLMConfig(
    String provider,                    // "fake" | "gemini" | "vertex" | "custom"
    String model,
    Duration timeout,                   // default 5s
    int maxRetries,                     // default 2
    Budget budget,                      // maxTokens, maxCostUSD, rateLimit
    CircuitBreakerConfig circuitBreaker, // threshold, timeout, halfOpenRequests
    boolean offlineDefault              // default true
) {}

public record Budget(
    int maxTokens,                      // default 10000
    double maxCostUSD,                  // default 0.50
    int rateLimitPerMinute              // default 10
) {}

public record CircuitBreakerConfig(
    double failureThreshold,            // default 0.5 (50%)
    int minimumCalls,                   // default 10
    Duration openTimeout,               // default 30s
    int halfOpenRequests                // default 3
) {}
```

## ProposalMetadata

```java
package org.shark.renovatio.llm.domain;

public record ProposalMetadata(
    String providerName,
    String providerVersion,
    String modelName,
    String modelVersion,
    boolean isOffline,
    List<String> supportedKinds
) {}
```

## Exceptions

```java
// Base exception
public class LLMProviderException extends RuntimeException {
    private final String errorCode;
    private final boolean retryable;
}

// Timeout
public class LLMTimeoutException extends LLMProviderException {}

// Budget exceeded
public class LLMBudgetExceededException extends LLMProviderException {}

// Invalid schema
public class LLMInvalidSchemaException extends LLMProviderException {}

// Circuit breaker open
public class LLMServiceUnavailableException extends LLMProviderException {}
```

## FakeLLMProvider (Default Offline)

```java
@Component
@ConditionalOnProperty(name = "renovatio.llm.provider", havingValue = "fake", matchIfMissing = true)
public class FakeLLMProvider implements LLMProvider {

    private final Map<String, TypedProposal> templates = new ConcurrentHashMap<>();

    @Override
    public TypedProposal propose(ProposalRequest request) {
        // Template-based response por kind
        String templateKey = request.kind() + "#" + request.schema();
        return templates.computeIfAbsent(templateKey, k -> generateTemplate(request));
    }

    private TypedProposal generateTemplate(ProposalRequest request) {
        // Genera propuesta vacía pero válida según schema
        return TypedProposal.builder()
            .version("1.0")
            .kind(request.kind())
            .proposal(emptyProposalForKind(request.kind()))
            .confidence(0.0)
            .rationale("Fake provider - offline mode")
            .schema(request.schema())
            .metadata(ProposalMetadata.builder()
                .model("fake")
                .promptHash("sha256:fake")
                .inputHash(request.input().sourceHash())
                .outputHash("sha256:fake")
                .latencyMs(0)
                .tokensUsed(0)
                .cacheStatus("BYPASS")
                .build())
            .build();
    }
}
```

## GeminiLLMProvider (Remote)

```java
@Component
@ConditionalOnProperty(name = "renovatio.llm.provider", havingValue = "gemini")
public class GeminiLLMProvider implements LLMProvider {

    private final VertexAIClient vertexClient;
    private final LLMConfig config;
    private final CircuitBreaker circuitBreaker;
    private final BudgetEnforcer budgetEnforcer;

    @Override
    public TypedProposal propose(ProposalRequest request) {
        // 1. Check circuit breaker
        circuitBreaker.executeRunnable(() -> {
            // 2. Check budget
            budgetEnforcer.checkBudget(request.budget());

            // 3. Build prompt with sanitization
            String prompt = buildPrompt(request);

            // 4. Call Vertex AI with timeout
            VertexResponse response = vertexClient.generate(
                config.model(),
                prompt,
                config.timeout()
            );

            // 5. Parse and validate response
            TypedProposal proposal = parseResponse(response, request);

            // 6. Validate schema
            validateSchema(proposal, request.schema());

            return proposal;
        });
    }
}
```

## Configuration Properties

```yaml
renovatio:
  llm:
    provider: fake                    # fake | gemini | vertex | custom
    model: gemini-2.0-flash
    timeout: 5000                     # ms
    max-retries: 2
    budget:
      max-tokens: 10000
      max-cost-usd: 0.50
      rate-limit-per-minute: 10
    circuit-breaker:
      failure-threshold: 0.5          # 50%
      minimum-calls: 10
      open-timeout: 30000             # ms
      half-open-requests: 3
    offline-default: true
```

## Versioning Policy

| Schema | Versioning | Compatibility |
|--------|------------|---------------|
| ProposalRequest | SemVer (major.minor) | Minor backward compatible |
| TypedProposal | SemVer (major.minor) | Minor backward compatible |
| LLMProvider interface | SemVer | Major = breaking |

## Testing Contract

### Valid Request Examples

```json
{
  "version": "1.0",
  "kind": "cobol.structure_analysis",
  "input": {
    "sourceHash": "sha256:a1b2c3d4...",
    "context": { "programName": "BATCH001", "hasCopybooks": false }
  },
  "schema": "proposal-request.v1.json",
  "budget": { "maxTokens": 5000, "maxLatencyMs": 5000 },
  "model": { "name": "gemini-2.0-flash", "version": "2025-01" }
}
```

### Expected Response

```json
{
  "version": "1.0",
  "kind": "cobol.structure_analysis",
  "proposal": {
    "programName": "BATCH001",
    "paragraphs": ["INITIALIZE", "PROCESS-RECORD", "FINALIZE"],
    "dataItems": ["WS-COUNTER", "WS-TOTAL"],
    "controlFlow": ["SEQUENTIAL", "IF-THEN-ELSE"]
  },
  "confidence": 0.92,
  "rationale": "Identified 3 paragraphs, 2 data items, standard batch pattern",
  "schema": "typed-proposal.v1.json",
  "metadata": {
    "model": "gemini-2.0-flash",
    "promptHash": "sha256:...",
    "inputHash": "sha256:a1b2c3d4...",
    "outputHash": "sha256:...",
    "latencyMs": 1234,
    "tokensUsed": 847,
    "cacheStatus": "MISS"
  }
}
```