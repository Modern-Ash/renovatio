---
schema: "agora/work/v1"
id: "application-orchestration-pipeline"
swarm: "issue-226-application-pipeline"
title: "AC-05: Crear un \u00fanico pipeline de aplicaci\u00f3n para preview y apply"
state: "clarified"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"use-cases":"Casos de uso p\u00fablicos cubren create/analyze project, review domain, resolve decisions, plan, preview, validate, apply y export evidence.","ports":"SourceAnalyzer, ProposalProvider, ArchitectureProjector, TargetEmitter, TargetRefiner, ValidationGate y repositorios de proyecto/artifacts son contratos expl\u00edcitos y m\u00ednimos.","side-effects":"Filesystem, Git, red, persistencia y reloj est\u00e1n detr\u00e1s de puertos; plan y preview no escriben en el workspace fuente.","idempotency":"Cada comando mutable posee idempotency key o sem\u00e1ntica equivalente y apply rechaza un manifest cuyo source hash qued\u00f3 obsoleto.","atomicity":"Apply escribe at\u00f3micamente o revierte sin dejar artifacts parciales; el ChangeSet conserva preimage, postimage y manifest.","thin-adapters":"API, CLI y MCP llaman la misma capa application y no contienen decisiones de migraci\u00f3n.","contract-tests":"Existe una suite reutilizable que valida implementaciones de puertos, preview/apply parity y determinismo."}
satisfied-criteria: []
criterion-statuses: {"use-cases":["specified"],"ports":["specified"],"side-effects":["specified"],"idempotency":["specified"],"atomicity":["specified"],"thin-adapters":["specified"],"contract-tests":["specified"]}
required-artifacts: ["spec","implementation-plan","application-contract","architecture-report","test-report"]
child-work-refs: []
budget-limits: null
---

# AC-05: Crear un único pipeline de aplicación para preview y apply

## Description

Issue #226. Introducir una capa application única que orqueste análisis, dominio, decisiones, planificación, emisión, validación y aplicación; mantener transports y renderers target fuera de esta capa.

## Acceptance criteria

- [ ] **use-cases:** Casos de uso públicos cubren create/analyze project, review domain, resolve decisions, plan, preview, validate, apply y export evidence.; stages: specified
- [ ] **ports:** SourceAnalyzer, ProposalProvider, ArchitectureProjector, TargetEmitter, TargetRefiner, ValidationGate y repositorios de proyecto/artifacts son contratos explícitos y mínimos.; stages: specified
- [ ] **side-effects:** Filesystem, Git, red, persistencia y reloj están detrás de puertos; plan y preview no escriben en el workspace fuente.; stages: specified
- [ ] **idempotency:** Cada comando mutable posee idempotency key o semántica equivalente y apply rechaza un manifest cuyo source hash quedó obsoleto.; stages: specified
- [ ] **atomicity:** Apply escribe atómicamente o revierte sin dejar artifacts parciales; el ChangeSet conserva preimage, postimage y manifest.; stages: specified
- [ ] **thin-adapters:** API, CLI y MCP llaman la misma capa application y no contienen decisiones de migración.; stages: specified
- [ ] **contract-tests:** Existe una suite reutilizable que valida implementaciones de puertos, preview/apply parity y determinismo.; stages: specified

## Required artifacts

- spec
- implementation-plan
- application-contract
- architecture-report
- test-report
