---
schema: "agora/work/v1"
id: "jcl-decoupling-integration"
swarm: "architecture-convergence-2026"
title: "AC-12: Desacoplar e integrar JCL en el pipeline can\u00f3nico"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"deterministic-core":"Parsing de JOB/EXEC/DD, COND/IF, datasets y utilities no depende de renovatio-llm ni de red.","semantic-projection":"JCL proyecta BatchJob y relaciones con programas/datasets al Semantic IR can\u00f3nico con identidad y provenance estables.","proposal-adapter":"Ambig\u00fcedades batch pueden solicitar TypedProposal mediante el puerto com\u00fan, pero aceptar/rechazar queda en DecisionSet.","application-flow":"Analyze/plan/preview de un proyecto combinado COBOL+JCL se ejecuta desde application services sin path separado.","spring-batch":"batch.target=spring-batch produce un manifest determinista o Action Items expl\u00edcitos para constructs no soportados.","fixtures":"Fixtures redistribuibles cubren steps, COND/IF, SORT/IDCAMS, datasets y referencias COBOL faltantes.","characterization":"El guardrail de caracterizaci\u00f3n requerido por la constituci\u00f3n se ejecuta y registra para cualquier cambio del path de generaci\u00f3n."}
satisfied-criteria: ["deterministic-core","semantic-projection","proposal-adapter","application-flow","spring-batch","fixtures","characterization"]
criterion-statuses: {"deterministic-core":["specified","planned","implemented","verified","accepted"],"semantic-projection":["specified","planned","implemented","verified","accepted"],"proposal-adapter":["specified","planned","implemented","verified","accepted"],"application-flow":["specified","planned","implemented","verified","accepted"],"spring-batch":["specified","planned","implemented","verified","accepted"],"fixtures":["specified","planned","implemented","verified","accepted"],"characterization":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","architecture-report","integration-report","test-report"]
child-work-refs: []
budget-limits: null
---

# AC-12: Desacoplar e integrar JCL en el pipeline canónico

## Description

Conservar el modelo JCL determinista, desacoplarlo del runtime LLM e integrarlo al pipeline canónico de application services.

## Acceptance criteria

- [x] **deterministic-core:** Parsing de JOB/EXEC/DD, COND/IF, datasets y utilities no depende de renovatio-llm ni de red.; stages: specified, planned, implemented, verified, accepted
- [x] **semantic-projection:** JCL proyecta BatchJob y relaciones con programas/datasets al Semantic IR canónico con identidad y provenance estables.; stages: specified, planned, implemented, verified, accepted
- [x] **proposal-adapter:** Ambigüedades batch pueden solicitar TypedProposal mediante el puerto común, pero aceptar/rechazar queda en DecisionSet.; stages: specified, planned, implemented, verified, accepted
- [x] **application-flow:** Analyze/plan/preview de un proyecto combinado COBOL+JCL se ejecuta desde application services sin path separado.; stages: specified, planned, implemented, verified, accepted
- [x] **spring-batch:** batch.target=spring-batch produce un manifest determinista o Action Items explícitos para constructs no soportados.; stages: specified, planned, implemented, verified, accepted
- [x] **fixtures:** Fixtures redistribuibles cubren steps, COND/IF, SORT/IDCAMS, datasets y referencias COBOL faltantes.; stages: specified, planned, implemented, verified, accepted
- [x] **characterization:** El guardrail de caracterización requerido por la constitución se ejecuta y registra para cualquier cambio del path de generación.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- architecture-report
- integration-report
- test-report
