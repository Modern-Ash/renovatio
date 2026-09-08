---
schema: "agora/work/v1"
id: "initialize-set-level88"
swarm: "issue-206-data-verbs-cycle-2"
title: "Implementar INITIALIZE y SET level-88 (#206 ciclo 2)"
state: "implementing"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"typed-ir":"INITIALIZE y SET level-88 se representan con nodos IR expl\u00edcitos y validaci\u00f3n de constructor.","initialize-semantics":"INITIALIZE emite defaults deterministas compatibles con el tipo PIC para todos sus destinos.","set-level88-semantics":"SET condition-name TO TRUE/FALSE resuelve el padre level-88 y emite el valor COBOL declarado sin inventar sem\u00e1ntica.","unsupported-visible":"Variantes SET/INITIALIZE no resolubles permanecen como ManualActionItem estable, nunca se descartan.","characterization":"Fixtures de caracterizaci\u00f3n cubren INITIALIZE y SET level-88 con Java esperado y comportamiento.","coverage-delta":"El reporte CardDemo se recalcula y documenta el delta de acciones manuales/emisi\u00f3n.","regression-green":"IR, OpenRewrite y provider-cobol quedan verdes."}
satisfied-criteria: []
criterion-statuses: {"typed-ir":["specified","planned","implemented"],"initialize-semantics":["specified","planned","implemented"],"set-level88-semantics":["specified","planned","implemented"],"unsupported-visible":["specified","planned","implemented"],"characterization":["specified","planned","implemented"],"coverage-delta":["specified","planned","implemented"],"regression-green":["specified","planned","implemented"]}
required-artifacts: ["spec","implementation-plan","verification-report","coverage-report"]
child-work-refs: []
budget-limits: null
---

# Implementar INITIALIZE y SET level-88 (#206 ciclo 2)

## Description

Añadir representación IR tipada y traducción determinista para INITIALIZE y SET condition-name TO TRUE/FALSE; conservar variantes no soportadas como acciones manuales y medir el delta CardDemo.

## Acceptance criteria

- [ ] **typed-ir:** INITIALIZE y SET level-88 se representan con nodos IR explícitos y validación de constructor.; stages: specified, planned, implemented
- [ ] **initialize-semantics:** INITIALIZE emite defaults deterministas compatibles con el tipo PIC para todos sus destinos.; stages: specified, planned, implemented
- [ ] **set-level88-semantics:** SET condition-name TO TRUE/FALSE resuelve el padre level-88 y emite el valor COBOL declarado sin inventar semántica.; stages: specified, planned, implemented
- [ ] **unsupported-visible:** Variantes SET/INITIALIZE no resolubles permanecen como ManualActionItem estable, nunca se descartan.; stages: specified, planned, implemented
- [ ] **characterization:** Fixtures de caracterización cubren INITIALIZE y SET level-88 con Java esperado y comportamiento.; stages: specified, planned, implemented
- [ ] **coverage-delta:** El reporte CardDemo se recalcula y documenta el delta de acciones manuales/emisión.; stages: specified, planned, implemented
- [ ] **regression-green:** IR, OpenRewrite y provider-cobol quedan verdes.; stages: specified, planned, implemented

## Required artifacts

- spec
- implementation-plan
- verification-report
- coverage-report
