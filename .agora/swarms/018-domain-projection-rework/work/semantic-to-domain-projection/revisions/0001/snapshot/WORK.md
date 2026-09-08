---
schema: "agora/work/v1"
id: "semantic-to-domain-projection"
swarm: "domain-projection-rework"
title: "Proyectar SemanticProgram al DomainModel"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"deterministic-mapping":"Data items, I/O, paragraphs, effects and control-flow produce domain nodes/relations using stable deterministic rules.","evidence":"Every projected node retains sourceRef/provenance and unsupported or ambiguous mappings become explicit action items.","llm-boundary":"LLM suggestions are schema-validated, cacheable, attributed and stored as NEEDS_REVIEW decisions; no final code is generated.","multi-program":"Multiple programs and shared copybooks preserve project isolation and deterministic identity.","regression":"Focused projection tests, Maven build and diff-check pass; existing generation path remains unchanged."}
satisfied-criteria: ["deterministic-mapping","evidence","llm-boundary","multi-program","regression"]
criterion-statuses: {"deterministic-mapping":["specified","planned","implemented","verified","accepted"],"evidence":["specified","planned","implemented","verified","accepted"],"llm-boundary":["specified","planned","implemented","verified","accepted"],"multi-program":["specified","planned","implemented","verified","accepted"],"regression":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Proyectar SemanticProgram al DomainModel

## Description

Mapear el Semantic IR existente a un modelo abstracto de negocio con evidencia y acciones manuales; permitir sugerencias LLM acotadas sin mutación automática.

## Acceptance criteria

- [x] **deterministic-mapping:** Data items, I/O, paragraphs, effects and control-flow produce domain nodes/relations using stable deterministic rules.; stages: specified, planned, implemented, verified, accepted
- [x] **evidence:** Every projected node retains sourceRef/provenance and unsupported or ambiguous mappings become explicit action items.; stages: specified, planned, implemented, verified, accepted
- [x] **llm-boundary:** LLM suggestions are schema-validated, cacheable, attributed and stored as NEEDS_REVIEW decisions; no final code is generated.; stages: specified, planned, implemented, verified, accepted
- [x] **multi-program:** Multiple programs and shared copybooks preserve project isolation and deterministic identity.; stages: specified, planned, implemented, verified, accepted
- [x] **regression:** Focused projection tests, Maven build and diff-check pass; existing generation path remains unchanged.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- verification-report
- review-report
