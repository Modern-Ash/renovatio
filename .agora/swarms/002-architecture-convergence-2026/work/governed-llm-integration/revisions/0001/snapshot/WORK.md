---
schema: "agora/work/v1"
id: "governed-llm-integration"
swarm: "architecture-convergence-2026"
title: "AC-08: Integrar el LLM como proveedor gobernado de propuestas"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"bounded-role":"El LLM solo recibe ProposalRequest acotados y devuelve TypedProposal validadas; no escribe filesystem, artifacts finales ni decisiones confirmadas","module-boundary":"Runtime/policies gen\u00e9ricos se separan de adapters y prompts COBOL; JCL y dominio determinista no importan SDKs o runtime LLM","runtime-config":"API y application soportan runtime offline por defecto y runtime remoto expl\u00edcitamente configurado, con timeout, retry acotado, rate/budget limits y circuit breaker","governance":"Cada llamada registra prompt/schema/model/version/input hash/output hash/cache status/actor y decisi\u00f3n humana sin persistir secretos","review":"Propuestas de baja confianza o categor\u00edas de alto riesgo requieren aceptaci\u00f3n; rechazo y fallback son estados expl\u00edcitos reproducibles","evaluation":"Un corpus versionado mide schema validity, determinism/replay, acceptance quality, unsafe output y regresi\u00f3n por prompt/model","failure-modes":"Ausencia del proveedor, timeout, respuesta inv\u00e1lida o presupuesto agotado no bloquea el n\u00facleo determinista y produce diagn\u00f3stico/action item estable","security":"El threat model cubre prompt injection desde source, exfiltraci\u00f3n, excessive agency, data minimization y retenci\u00f3n"}
satisfied-criteria: ["bounded-role","module-boundary","runtime-config","governance","review","evaluation","failure-modes","security"]
criterion-statuses: {"bounded-role":["specified","planned","implemented","verified","accepted"],"module-boundary":["specified","planned","implemented","verified","accepted"],"runtime-config":["specified","planned","implemented","verified","accepted"],"governance":["specified","planned","implemented","verified","accepted"],"review":["specified","planned","implemented","verified","accepted"],"evaluation":["specified","planned","implemented","verified","accepted"],"failure-modes":["specified","planned","implemented","verified","accepted"],"security":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","runtime-contract","threat-model","evaluation-report","test-report"]
child-work-refs: []
budget-limits: null
---

# AC-08: Integrar el LLM como proveedor gobernado de propuestas

## Description

Integrar el LLM como proveedor gobernado de propuestas acotadas

## Acceptance criteria

- [x] **bounded-role:** El LLM solo recibe ProposalRequest acotados y devuelve TypedProposal validadas; no escribe filesystem, artifacts finales ni decisiones confirmadas; stages: specified, planned, implemented, verified, accepted
- [x] **module-boundary:** Runtime/policies genéricos se separan de adapters y prompts COBOL; JCL y dominio determinista no importan SDKs o runtime LLM; stages: specified, planned, implemented, verified, accepted
- [x] **runtime-config:** API y application soportan runtime offline por defecto y runtime remoto explícitamente configurado, con timeout, retry acotado, rate/budget limits y circuit breaker; stages: specified, planned, implemented, verified, accepted
- [x] **governance:** Cada llamada registra prompt/schema/model/version/input hash/output hash/cache status/actor y decisión humana sin persistir secretos; stages: specified, planned, implemented, verified, accepted
- [x] **review:** Propuestas de baja confianza o categorías de alto riesgo requieren aceptación; rechazo y fallback son estados explícitos reproducibles; stages: specified, planned, implemented, verified, accepted
- [x] **evaluation:** Un corpus versionado mide schema validity, determinism/replay, acceptance quality, unsafe output y regresión por prompt/model; stages: specified, planned, implemented, verified, accepted
- [x] **failure-modes:** Ausencia del proveedor, timeout, respuesta inválida o presupuesto agotado no bloquea el núcleo determinista y produce diagnóstico/action item estable; stages: specified, planned, implemented, verified, accepted
- [x] **security:** El threat model cubre prompt injection desde source, exfiltración, excessive agency, data minimization y retención; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- runtime-contract
- threat-model
- evaluation-report
- test-report
