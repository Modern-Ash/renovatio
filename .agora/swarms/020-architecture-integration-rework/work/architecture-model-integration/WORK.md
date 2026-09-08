---
schema: "agora/work/v1"
id: "architecture-model-integration"
swarm: "architecture-integration-rework"
title: "Integrar ArchitectureModel con preview y emitters"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"contract-adapter":"Existe un adaptador expl\u00edcito desde ArchitectureModel hacia contratos actuales","preview-consumption":"El preview puede consumir la proyecci\u00f3n sin reinterpretar COBOL","deterministic":"La integraci\u00f3n conserva determinismo y provenance","regression":"Tests Maven y diff-check pasan"}
satisfied-criteria: ["contract-adapter","preview-consumption","deterministic","regression"]
criterion-statuses: {"contract-adapter":["specified","planned","implemented","verified","accepted"],"preview-consumption":["specified","planned","implemented","verified","accepted"],"deterministic":["specified","planned","implemented","verified","accepted"],"regression":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Integrar ArchitectureModel con preview y emitters

## Description

Conectar la proyección DomainModel con los contratos de arquitectura y consumo existentes, preservando compatibilidad.

## Acceptance criteria

- [x] **contract-adapter:** Existe un adaptador explícito desde ArchitectureModel hacia contratos actuales; stages: specified, planned, implemented, verified, accepted
- [x] **preview-consumption:** El preview puede consumir la proyección sin reinterpretar COBOL; stages: specified, planned, implemented, verified, accepted
- [x] **deterministic:** La integración conserva determinismo y provenance; stages: specified, planned, implemented, verified, accepted
- [x] **regression:** Tests Maven y diff-check pasan; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- verification-report
- review-report
