---
schema: "agora/work/v1"
id: "cobol-target-equivalence"
swarm: "equivalence-validation-rework"
title: "Replay y shadow validation antes de cutover"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"replay":"Casos COBOL reproducibles se ejecutan contra ambas implementaciones","comparison":"Se comparan salidas y efectos con tolerancias expl\u00edcitas","gate":"El cutover requiere umbrales y aprobaci\u00f3n","evidence":"Resultados y divergencias quedan trazados"}
satisfied-criteria: ["replay","comparison","gate","evidence"]
criterion-statuses: {"replay":["specified","planned","implemented","verified","accepted"],"comparison":["specified","planned","implemented","verified","accepted"],"gate":["specified","planned","implemented","verified","accepted"],"evidence":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Replay y shadow validation antes de cutover

## Description

Definir y ejecutar comparación reproducible de entradas, salidas, efectos y errores entre COBOL y el destino.

## Acceptance criteria

- [x] **replay:** Casos COBOL reproducibles se ejecutan contra ambas implementaciones; stages: specified, planned, implemented, verified, accepted
- [x] **comparison:** Se comparan salidas y efectos con tolerancias explícitas; stages: specified, planned, implemented, verified, accepted
- [x] **gate:** El cutover requiere umbrales y aprobación; stages: specified, planned, implemented, verified, accepted
- [x] **evidence:** Resultados y divergencias quedan trazados; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- verification-report
- review-report
