---
schema: "agora/clarifications/v1"
swarm: "architecture-convergence-2026"
work: "governed-llm-integration"
created-at: "2026-09-10T16:30:52.924274Z"
last-run-input-sha256: "2cb0f8e0caea6e447da4cbe80c5fcb291ace56e431f57d0383c013a10e998b59"
last-run-question-count: 5
last-run-unanswered-count: 0
last-run-by: "project:owner"
last-run-at: "2026-09-10T16:30:52.924274Z"
---

# Clarifications for governed-llm-integration

| Question | Answer | Actor | Timestamp | Input SHA-256 |
| --- | --- | --- | --- | --- |
| ¿Qué categorías concretas de ProposalRequest/TypedProposal quedan dentro del alcance, qué esquema versionado corresponde a cada una y cuáles se consideran de alto riesgo? | 3 categorías v1: cobol.structure_analysis (análisis de estructura programa), cobol.business_rule (extracción reglas de negocio), cobol.data_mapping (mapeo copybook→JPA entity). Alto riesgo: business_rule (afecta lógica), data_mapping (afecta persistencia). structure_analysis: bajo riesgo. | project:owner | 2026-09-10T16:30:52.924274Z | 2cb0f8e0caea6e447da4cbe80c5fcb291ace56e431f57d0383c013a10e998b59 |
| ¿Qué umbral y método determinan una propuesta de «baja confianza», y qué actor puede aceptar o rechazar propuestas de baja confianza o alto riesgo? | Umbral: confidence < 0.75 o schema validation warnings. Actor: spec-owner (project:owner) decide accept/reject/fallback. Fallback: fake provider output + ActionItem. | project:owner | 2026-09-10T16:30:52.924274Z | 2cb0f8e0caea6e447da4cbe80c5fcb291ace56e431f57d0383c013a10e998b59 |
| ¿Dónde se persistirá el registro de gobernanza, qué política de retención y redacción se aplicará, y cómo se representará de forma reproducible la decisión humana sin almacenar secretos? | Governance log: renovatio-llm/audit/ con retention 90d. Redaction: PII, API keys, source snippets >50 chars. Decisión: DecisionSet record con decisionId, actor, timestamp, rationaleHash (no content). | project:owner | 2026-09-10T16:30:52.924274Z | 2cb0f8e0caea6e447da4cbe80c5fcb291ace56e431f57d0383c013a10e998b59 |
| ¿Cuáles son los valores y límites verificables para timeout, reintentos, tasa, presupuesto y circuit breaker, y qué diagnóstico/action item estable debe producir cada modo de fallo? | timeout=5s, maxRetries=2, rate=10/min, budget=10k tokens/call, $0.50/call. Circuit breaker: 50% errors/10 calls → open 30s. Fallos: provider_missing→fake+AI, timeout→retry→fake+AI, invalid_schema→reject+AI, budget_exceeded→cb_open+AI, rate_limit→backoff+fake+AI. | project:owner | 2026-09-10T16:30:52.924274Z | 2cb0f8e0caea6e447da4cbe80c5fcb291ace56e431f57d0383c013a10e998b59 |
| ¿Qué corpus versionado, métricas, umbrales de aprobación y matriz de prompts/modelos deben cumplir el evaluation-report y el test-report para satisfacer el criterio de evaluación? | Corpus: evals/corpus/v1/ 50 casos COBOL (10 por categoría). Métricas: schema_validity>99%, unsafe_output=0%, acceptance_quality>80%, regression=0%. Modelos: gemini-2.0-flash, vertex-gemini-pro. Test matrix: 3 categorías x 2 modelos x 3 failure modes = 18 contract tests + 50 eval cases. | project:owner | 2026-09-10T16:30:52.924274Z | 2cb0f8e0caea6e447da4cbe80c5fcb291ace56e431f57d0383c013a10e998b59 |