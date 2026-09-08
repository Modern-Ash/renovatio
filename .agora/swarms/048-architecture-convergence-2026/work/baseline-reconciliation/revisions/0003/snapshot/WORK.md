---
schema: "agora/work/v1"
id: "baseline-reconciliation"
swarm: "architecture-convergence-2026"
title: "AC-01: reconciliar ramas y establecer baseline can\u00f3nica (#222)"
state: "completed"
revision: 3
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"inventory":"Existe un inventario reproducible por series coherentes, clasificado como portar, ya-presente, obsoleto o descartar, con justificaci\u00f3n y owner.","clean-integration":"La rama parte de origin/main y no contiene conflictos, cambios accidentales ni p\u00e9rdida de trabajo del usuario; s\u00f3lo se portan cambios aprobados por el inventario.","mvc-contract":"La decisi\u00f3n MVC/CICS vigente en main queda documentada y sus tres pruebas focalizadas permanecen verdes.","history-integrity":"SHAs de entrada, merge-base, range-diff, patch-id, commits portados/descartados y relaci\u00f3n entre las \u00e9picas #205/#221 quedan registrados de forma reproducible.","baseline-tests":"El reactor alcanza el primer fallo real de main sin regresiones introducidas; las pruebas relevantes y su salida quedan registradas.","rollback":"Existe una referencia recuperable al estado previo y un procedimiento documentado que no requiere reescritura destructiva."}
satisfied-criteria: ["inventory","clean-integration","mvc-contract","history-integrity","baseline-tests","rollback"]
criterion-statuses: {"inventory":["specified","planned","implemented","verified","accepted"],"clean-integration":["specified","planned","implemented","verified","accepted"],"mvc-contract":["specified","planned","implemented","verified","accepted"],"history-integrity":["specified","planned","implemented","verified","accepted"],"baseline-tests":["specified","planned","implemented","verified","accepted"],"rollback":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","integration-report","decision-record","test-report"]
child-work-refs: []
budget-limits: null
---

# AC-01: reconciliar ramas y establecer baseline canónica (#222)

## Description

Partir del origin/main posterior a #207, inventariar y clasificar las ramas/series exclusivas, portar únicamente cambios intencionales, preservar rollback y documentar cómo el trabajo pendiente de la épica #205 se integra en #221/#228. La rama actual de #217 ya es ancestro de main; la divergencia material reside principalmente en renovatio-workbench-bootstrap y decision-engine-f8.

## Acceptance criteria

- [x] **inventory:** Existe un inventario reproducible por series coherentes, clasificado como portar, ya-presente, obsoleto o descartar, con justificación y owner.; stages: specified, planned, implemented, verified, accepted
- [x] **clean-integration:** La rama parte de origin/main y no contiene conflictos, cambios accidentales ni pérdida de trabajo del usuario; sólo se portan cambios aprobados por el inventario.; stages: specified, planned, implemented, verified, accepted
- [x] **mvc-contract:** La decisión MVC/CICS vigente en main queda documentada y sus tres pruebas focalizadas permanecen verdes.; stages: specified, planned, implemented, verified, accepted
- [x] **history-integrity:** SHAs de entrada, merge-base, range-diff, patch-id, commits portados/descartados y relación entre las épicas #205/#221 quedan registrados de forma reproducible.; stages: specified, planned, implemented, verified, accepted
- [x] **baseline-tests:** El reactor alcanza el primer fallo real de main sin regresiones introducidas; las pruebas relevantes y su salida quedan registradas.; stages: specified, planned, implemented, verified, accepted
- [x] **rollback:** Existe una referencia recuperable al estado previo y un procedimiento documentado que no requiere reescritura destructiva.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- integration-report
- decision-record
- test-report
