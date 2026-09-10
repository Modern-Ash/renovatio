# Implementation Plan: Integrar el LLM como Proveedor Gobernado de Propuestas

## Fases

### Fase 1: Runtime Contracts (Semana 1)
**Objetivo**: Definir ProposalRequest, TypedProposal, LLMProvider interface, schemas JSON v1

**Entregables**:
- `renovatio-shared/src/main/resources/schemas/proposal-request.v1.json`
- `renovatio-shared/src/main/resources/schemas/typed-proposal.v1.json`
- `renovatio-llm/src/main/java/.../llm/LLMProvider.java` (interface)
- `renovatio-llm/src/main/java/.../llm/LLMConfig.java` (record)
- `renovatio-llm/src/main/java/.../llm/FakeLLMProvider.java` (default offline)
- Tests de validación de schemas

**Criterios**: `bounded-role` (specified→planned), `runtime-config` (specified→planned)

---

### Fase 2: Module Boundaries (Semana 2)
**Objetivo**: Separar runtime/policies genéricos de adapters COBOL/JCL

**Entregables**:
- Mover `renovatio-llm` runtime a módulo independiente `renovatio-llm-runtime`
- Adapters COBOL en `renovatio-provider-cobol/src/main/java/.../llm/CobolLLMAdapter.java`
- Adapters JCL en `renovatio-provider-jcl/src/main/java/.../llm/JclLLMAdapter.java`
- Eliminar dependencias LLM del dominio determinista (COBOL IR, JCL core)
- ArchUnit tests: `NoLLMDependencyInCoreTest`

**Criterios**: `module-boundary` (specified→planned)

---

### Fase 3: Governance & DecisionSet Integration (Semana 3)
**Objetivo**: Integrar LLM en pipeline application con gobernanza completa

**Entregables**:
- `renovatio-application/src/main/java/.../governance/GovernanceLogger.java`
- `renovatio-application/src/main/java/.../decision/DecisionSet.java` (extensión)
- `renovatio-application/src/main/java/.../llm/LLMProposalStage.java` (pipeline stage)
- Redaction util: `GovernanceRedactor.java`
- Decision record con `decisionId`, `actor`, `timestamp`, `rationaleHash`
- Audit log writer con retention 90d

**Criterios**: `governance` (specified→planned), `review` (specified→planned)

---

### Fase 4: Runtime Configuration & Failure Modes (Semana 4)
**Objetivo**: Configuración explícita, circuit breaker, budgets, failure modes

**Entregables**:
- `renovatio-llm/src/main/java/.../config/LLMConfigLoader.java` (Spring @ConfigurationProperties)
- `renovatio-llm/src/main/java/.../resilience/CircuitBreakerLLMProvider.java` (resilience4j wrapper)
- `renovatio-llm/src/main/java/.../resilience/BudgetEnforcer.java`
- Remote provider: `GeminiLLMProvider.java` (implementa LLMProvider)
- Cache con `cacheStatus` en metadata
- Failure mode tests: 5 escenarios

**Criterios**: `runtime-config` (planned→implemented), `failure-modes` (specified→planned→implemented)

---

### Fase 5: Threat Model & Security (Semana 5)
**Objetivo**: STRIDE completo, data minimization, security tests

**Entregables**:
- `docs/security/threat-model-llm.md` (STRIDE detallado)
- `renovatio-llm/src/main/java/.../security/PromptSanitizer.java`
- `renovatio-llm/src/main/java/.../security/DataMinimizer.java` (context trimming)
- Tests de prompt injection, exfiltration, data minimization
- Dependency scan: no SDKs LLM en core modules

**Criterios**: `security` (specified→planned→implemented)

---

### Fase 6: Evaluation Corpus & Tests (Semana 6)
**Objetivo**: Corpus versionado, evals, contract tests, regression

**Entregables**:
- `renovatio-llm/src/test/resources/evals/corpus/v1/` (50 casos)
- `renovatio-llm/src/test/java/.../EvaluationTest.java`
- `renovatio-llm/src/test/java/.../LLMProviderContractTest.java` (18 tests)
- `renovatio-llm/src/test/java/.../FailureModeTest.java` (5 tests)
- CI pipeline: evals en PR, regression gate

**Criterios**: `evaluation` (specified→planned→implemented)

---

### Fase 7: Integration & Documentation (Semana 7)
**Objetivo**: End-to-end integration, runbook, public docs

**Entregables**:
- Integration test: `LLME2ETest.java` (pipeline completo con fake provider)
- `docs/RUNBOOK-LLM.md` (setup <10 min, offline demo)
- `docs/ARCHITECTURE-LLM.md` (module boundaries, data flow)
- Update `CLAUDE.md` con LLM governance patterns
- Evidence submission

**Criterios**: Todos a `verified` → `accepted`

---

## Dependencias

```
Fase 1 (Contracts)
    ├──→ Fase 2 (Boundaries)
    │       ├──→ Fase 3 (Governance)
    │       │       ├──→ Fase 4 (Config/Failure)
    │       │       └──→ Fase 5 (Security)
    │       └──→ Fase 6 (Evaluation)
    └──→ Fase 7 (Integration)
```

## Riesgos y Mitigación

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Fake provider insufficient for evals | Media | Alto | Implementar fake con templates parametrizados |
| Circuit breaker config tuning | Alta | Medio | Exponer métricas Prometheus, alertas |
| Schema evolution breaking changes | Media | Alto | Versionado estricto, migration tests |
| Prompt injection bypass | Baja | Crítico | Defense in depth: sanitize + validate + no exec |

## Definition of Done por Fase

Cada fase requiere:
- Código compila y tests pasan (mvn verify)
- ArchUnit tests pasan
- Evidence registrada en Agora
- Criterios movidos a `implemented` o `verified`
- PR con review de spec-owner