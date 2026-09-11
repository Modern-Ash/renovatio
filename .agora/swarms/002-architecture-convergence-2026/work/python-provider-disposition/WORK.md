---
schema: "agora/work/v1"
id: "python-provider-disposition"
swarm: "architecture-convergence-2026"
title: "AC-13: Resolver el destino del prototipo Python"
state: "verifying"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"assessment":"Inventario de capacidades reales del m\u00f3dulo Python, deuda, tests, dependencias y diferencias contra TargetEmitter.","decision":"ADR aprobado elige integrar, laboratorio separado o retirar, con costo, beneficio y consecuencias.","truthfulness":"README, capability schema y roadmap reflejan la decisi\u00f3n y no presentan Python como soportado sin evidencia.","integration-option":"Si se integra, PythonEmitter cumple contract tests, build, packaging, fixtures, equivalence, CLI/API y manifest determinista.","archive-option":"Si se archiva o retira, se preserva historia \u00fatil, se eliminan entrypoints enga\u00f1osos y se documenta reemplazo o estado unsupported.","ci-scope":"CI ejecuta los tests correspondientes al estado elegido y no deja un subproyecto silenciosamente fuera de gates declarados."}
satisfied-criteria: []
criterion-statuses: {"assessment":["specified","planned","implemented","verified"],"decision":["specified","planned","implemented","verified"],"truthfulness":["specified","planned","implemented","verified"],"integration-option":["specified","planned","implemented","verified"],"archive-option":["specified","planned","implemented","verified"],"ci-scope":["specified","planned","implemented","verified"]}
required-artifacts: ["spec","implementation-plan","capability-assessment","decision-record","disposition-report","test-report"]
child-work-refs: []
budget-limits: null
---

# AC-13: Resolver el destino del prototipo Python

## Description

Decidir y aplicar una disposición verificable para renovatio-provider-python: integrar, mantener como laboratorio separado o retirar/archivar, evitando presentarlo como TargetEmitter soportado sin evidencia.

## Acceptance criteria

- [ ] **assessment:** Inventario de capacidades reales del módulo Python, deuda, tests, dependencias y diferencias contra TargetEmitter.; stages: specified, planned, implemented, verified
- [ ] **decision:** ADR aprobado elige integrar, laboratorio separado o retirar, con costo, beneficio y consecuencias.; stages: specified, planned, implemented, verified
- [ ] **truthfulness:** README, capability schema y roadmap reflejan la decisión y no presentan Python como soportado sin evidencia.; stages: specified, planned, implemented, verified
- [ ] **integration-option:** Si se integra, PythonEmitter cumple contract tests, build, packaging, fixtures, equivalence, CLI/API y manifest determinista.; stages: specified, planned, implemented, verified
- [ ] **archive-option:** Si se archiva o retira, se preserva historia útil, se eliminan entrypoints engañosos y se documenta reemplazo o estado unsupported.; stages: specified, planned, implemented, verified
- [ ] **ci-scope:** CI ejecuta los tests correspondientes al estado elegido y no deja un subproyecto silenciosamente fuera de gates declarados.; stages: specified, planned, implemented, verified

## Required artifacts

- spec
- implementation-plan
- capability-assessment
- decision-record
- disposition-report
- test-report
