---
schema: "agora/work/v1"
id: "initialize-set-level88"
swarm: "issue-206-data-verbs-cycle-2"
title: "Implementar INITIALIZE y SET level-88 (#206 ciclo 2)"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"typed-ir":"INITIALIZE y SET level-88 se representan con nodos IR expl\u00edcitos y validaci\u00f3n de constructor.","initialize-semantics":"INITIALIZE emite defaults deterministas compatibles con el tipo PIC para todos sus destinos.","set-level88-semantics":"SET condition-name TO TRUE/FALSE resuelve el padre level-88 y emite el valor COBOL declarado sin inventar sem\u00e1ntica.","unsupported-visible":"Variantes SET/INITIALIZE no resolubles permanecen como ManualActionItem estable, nunca se descartan.","characterization":"Fixtures de caracterizaci\u00f3n cubren INITIALIZE y SET level-88 con Java esperado y comportamiento.","coverage-delta":"El reporte CardDemo se recalcula y documenta el delta de acciones manuales/emisi\u00f3n.","regression-green":"IR, OpenRewrite y provider-cobol quedan verdes."}
satisfied-criteria: ["typed-ir","initialize-semantics","set-level88-semantics","unsupported-visible","characterization","coverage-delta","regression-green"]
criterion-statuses: {"typed-ir":["specified","planned","implemented","verified","accepted"],"initialize-semantics":["specified","planned","implemented","verified","accepted"],"set-level88-semantics":["specified","planned","implemented","verified","accepted"],"unsupported-visible":["specified","planned","implemented","verified","accepted"],"characterization":["specified","planned","implemented","verified","accepted"],"coverage-delta":["specified","planned","implemented","verified","accepted"],"regression-green":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","verification-report","coverage-report"]
child-work-refs: []
budget-limits: null
---

# Implementar INITIALIZE y SET level-88 (#206 ciclo 2)

## Description

Añadir representación IR tipada y traducción determinista para INITIALIZE y SET condition-name TO TRUE/FALSE; conservar variantes no soportadas como acciones manuales y medir el delta CardDemo.

## Acceptance criteria

- [x] **typed-ir:** INITIALIZE y SET level-88 se representan con nodos IR explícitos y validación de constructor.; stages: specified, planned, implemented, verified, accepted
- [x] **initialize-semantics:** INITIALIZE emite defaults deterministas compatibles con el tipo PIC para todos sus destinos.; stages: specified, planned, implemented, verified, accepted
- [x] **set-level88-semantics:** SET condition-name TO TRUE/FALSE resuelve el padre level-88 y emite el valor COBOL declarado sin inventar semántica.; stages: specified, planned, implemented, verified, accepted
- [x] **unsupported-visible:** Variantes SET/INITIALIZE no resolubles permanecen como ManualActionItem estable, nunca se descartan.; stages: specified, planned, implemented, verified, accepted
- [x] **characterization:** Fixtures de caracterización cubren INITIALIZE y SET level-88 con Java esperado y comportamiento.; stages: specified, planned, implemented, verified, accepted
- [x] **coverage-delta:** El reporte CardDemo se recalcula y documenta el delta de acciones manuales/emisión.; stages: specified, planned, implemented, verified, accepted
- [x] **regression-green:** IR, OpenRewrite y provider-cobol quedan verdes.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- verification-report
- coverage-report
