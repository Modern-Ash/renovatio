# Spec: Integrar el LLM como Proveedor Gobernado de Propuestas

## Contexto

renovatio-llm posee buenas guardas internas, pero el API registra por defecto un SuggestionRuntime que falla cerradamente y no cablea un proveedor productivo. El módulo genérico depende de COBOL IR y JCL depende del módulo LLM, mezclando núcleo determinista con adapters de inteligencia.

## Problema

Conectar el runtime LLM al pipeline canónico sin otorgarle autoridad de transformación, preservando modo offline, esquemas, atribución y revisión. Depende de AC-04 y AC-05; puede avanzar en paralelo con AC-06.

## Arquitectura Objetivo

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Pipeline                       │
├─────────────────────────────────────────────────────────────┤
│  DecisionSet ←─ Proposal ←─ TypedProposal ←─ LLM Provider    │
│      │                                                        │
│      ├───► Deterministic Core (COBOL IR, JCL, Java Gen)      │
│      └───► Evidence/Attribution/Cache                        │
└─────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  Offline Fake   │  │  Remote Config  │  │  Circuit Breaker│
│  (Default)      │  │  (Explicit)     │  │  + Budgets      │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

## Contratos Nuevos

### ProposalRequest (versionado)
```json
{
  "version": "1.0",
  "kind": "cobol.structure_analysis|cobol.business_rule|cobol.data_mapping",
  "input": { "sourceHash": "...", "context": {...} },
  "schema": "proposal-request.v1.json",
  "budget": { "maxTokens": 2000, "maxLatencyMs": 5000 },
  "model": { "name": "gemini-2.0-flash", "version": "2025-01" }
}
```

### TypedProposal (versionado)
```json
{
  "version": "1.0",
  "kind": "cobol.structure_analysis",
  "proposal": { "entities": [...], "relationships": [...] },
  "confidence": 0.87,
  "rationale": "...",
  "schema": "typed-proposal.v1.json",
  "metadata": {
    "model": "gemini-2.0-flash",
    "promptHash": "sha256:...",
    "inputHash": "sha256:...",
    "latencyMs": 1234,
    "tokensUsed": 1500
  }
}
```

### LLMProvider (interface)
```java
public interface LLMProvider {
    TypedProposal propose(ProposalRequest request);
    ProposalMetadata metadata();
    void configure(LLMConfig config);
}
```

### LLMConfig
```java
public record LLMConfig(
    String provider,           // "fake" | "gemini" | "vertex" | "custom"
    String model,
    Duration timeout,
    int maxRetries,
    Budget budget,             // maxTokens, maxCostUSD, rateLimit
    CircuitBreakerConfig cb,   // threshold, timeout, halfOpenRequests
    boolean offlineDefault
) {}
```

## Pipeline de Gobernanza

1. **Request Construction**: Application construye `ProposalRequest` con `inputHash`, `schema`, `budget`
2. **Provider Selection**: Config determina `fake` (default) o proveedor remoto
3. **Execution**: `LLMProvider.propose()` con timeout y retry
4. **Validation**: `TypedProposal` validado contra schema JSON
5. **Attribution**: Registrar `promptHash`, `model`, `inputHash`, `outputHash`, `cacheStatus`
6. **DecisionSet**: Propuesta entra a `DecisionSet` para revisión humana
7. **Confirmation/Rejection**: `Decision` explícita con fallback reproducible

## Failure Modes

| Escenario | Comportamiento |
|-----------|----------------|
| Proveedor ausente | `fake` provider activo → propuesta vacía + ActionItem |
| Timeout | Retry acotado → fallback a `fake` + ActionItem |
| Schema inválido | Rechazo inmediato → ActionItem + no bloquea core |
| Budget agotado | Circuit breaker open → `fake` + ActionItem |
| Rate limit | Backoff exponencial → retry o fallback |

## Threat Model (STRIDE)

| Amenaza | Mitigación |
|---------|------------|
| **S**poofing (prompt injection) | Input sanitization, schema validation, no ejecutar LLM output |
| **T**ampering | Input/output hashes, attestation, immutable evidence log |
| **R**epudiation | Actor tracking, decision audit trail, signed decisions |
| **I**nformation Disclosure | Data minimization, no enviar source completo, PII redaction |
| **D**enial of Service | Budget limits, circuit breaker, timeout, rate limiting |
| **E**levation of Privilege | LLM sin autoridad de escritura, solo propone, humano decide |

## Evaluación (Corpus Versionado)

- **Dataset**: `evals/corpus/v1/` con 50+ casos COBOL→propuesta
- **Métricas**: schema validity (>99%), unsafe output (0%), acceptance quality (>80%), regression (0%)
- **Ejecución**: `mvn test -pl renovatio-llm -Dtest=EvaluationTest`

## Fuera de Alcance

- No permitir que el LLM emita archivos, ejecute recipes o aplique cambios
- No enviar repositorios completos cuando alcanza contexto mínimo
- No convertir una respuesta válida de schema en una decisión automáticamente aceptada
- No hacer obligatoria la red para análisis o generación

## Artefactos Agora

1. `spec` — Este documento
2. `implementation-plan` — Fases con dependencias
3. `runtime-contract` — ProposalRequest/TypedProposal schemas + LLMProvider interface
4. `threat-model` — STRIDE completo con mitigaciones
5. `evaluation-report` — Resultados sobre corpus versionado
6. `test-report` — Contract tests + failure mode tests