---
schema: "agora/work/v1"
id: "domain-model-neutral"
swarm: "domain-model-rework"
title: "Definir DomainModel neutral con evidencia COBOL"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"domain-contract":"DomainModel versionado con entidades, value objects, aggregates, use cases, services, repositories, external systems, events e invariants.","evidence-traceability":"Cada elemento conserva referencias COBOL, provenance, confidence y origin, con IDs deterministas.","validation":"El modelo valida referencias, duplicados, hashes y serializaci\u00f3n can\u00f3nica.","compatibility":"M\u00faltiples programas/copybooks son soportados y proyectos sin DomainModel mantienen el flujo actual.","regression-quality":"Schemas, documentaci\u00f3n y tests focalizados pasan con diff-check."}
satisfied-criteria: ["domain-contract","evidence-traceability","validation","compatibility","regression-quality"]
criterion-statuses: {"domain-contract":["specified","planned","implemented","verified","accepted"],"evidence-traceability":["specified","planned","implemented","verified","accepted"],"validation":["specified","planned","implemented","verified","accepted"],"compatibility":["specified","planned","implemented","verified","accepted"],"regression-quality":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Definir DomainModel neutral con evidencia COBOL

## Description

Introducir el contrato versionado del modelo abstracto de negocio entre Semantic IR y arquitectura destino, sin cambiar aún la generación existente.

## Acceptance criteria

- [x] **domain-contract:** DomainModel versionado con entidades, value objects, aggregates, use cases, services, repositories, external systems, events e invariants.; stages: specified, planned, implemented, verified, accepted
- [x] **evidence-traceability:** Cada elemento conserva referencias COBOL, provenance, confidence y origin, con IDs deterministas.; stages: specified, planned, implemented, verified, accepted
- [x] **validation:** El modelo valida referencias, duplicados, hashes y serialización canónica.; stages: specified, planned, implemented, verified, accepted
- [x] **compatibility:** Múltiples programas/copybooks son soportados y proyectos sin DomainModel mantienen el flujo actual.; stages: specified, planned, implemented, verified, accepted
- [x] **regression-quality:** Schemas, documentación y tests focalizados pasan con diff-check.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- verification-report
- review-report
