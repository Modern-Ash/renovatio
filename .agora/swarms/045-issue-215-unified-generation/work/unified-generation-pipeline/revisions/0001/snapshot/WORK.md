---
schema: "agora/work/v1"
id: "unified-generation-pipeline"
swarm: "issue-215-unified-generation"
title: "Unificar generaci\u00f3n COBOL\u2192Java en CLI/API (#215)"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"canonical-body-path":"Cada programa Java emitido por el camino productivo pasa por una \u00fanica funci\u00f3n de traducci\u00f3n sem\u00e1ntica; la ausencia de sidecar anotado no deja el cuerpo TODO cuando el IR base es v\u00e1lido.","cli-plan-apply":"renovatio plan seguido de apply --no-dry-run --out sobre un fixture soportado escribe Java traducido, sin TODO, y el contenido funcional coincide con la generaci\u00f3n directa.","api-lifecycle":"La integraci\u00f3n API/proveedor de plan y apply verifica que el resultado incluye l\u00f3gica COBOL traducida y archivos generados, no s\u00f3lo el ciclo de persistencia.","manual-actions":"El camino can\u00f3nico conserva un reporte \u00fanico de ManualActionItem y no oculta fallos de enriquecimiento como una generaci\u00f3n exitosa con TODO.","architecture-record":"Un ADR identifica la cadena can\u00f3nica, la responsabilidad de JavaGenerationService/TargetEmitter/OpenRewrite y los l\u00edmites legacy/deprecados.","regression-green":"Los m\u00f3dulos provider-cobol, CLI, API, OpenRewrite y el harness de caracterizaci\u00f3n quedan verdes."}
satisfied-criteria: ["canonical-body-path","cli-plan-apply","api-lifecycle","manual-actions","architecture-record","regression-green"]
criterion-statuses: {"canonical-body-path":["specified","planned","implemented","verified","accepted"],"cli-plan-apply":["specified","planned","implemented","verified","accepted"],"api-lifecycle":["specified","planned","implemented","verified","accepted"],"manual-actions":["specified","planned","implemented","verified","accepted"],"architecture-record":["specified","planned","implemented","verified","accepted"],"regression-green":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","architecture-decision-record","verification-report"]
child-work-refs: []
budget-limits: null
---

# Unificar generación COBOL→Java en CLI/API (#215)

## Description

Consolidar el camino productivo parse→IR→arquitectura→emisión→OpenRewrite para que plan/apply y generación directa produzcan la misma lógica traducida sin depender de sidecar anotado. Añadir pruebas CLI/API y ADR; conservar reporte de acciones manuales y compatibilidad.

## Acceptance criteria

- [x] **canonical-body-path:** Cada programa Java emitido por el camino productivo pasa por una única función de traducción semántica; la ausencia de sidecar anotado no deja el cuerpo TODO cuando el IR base es válido.; stages: specified, planned, implemented, verified, accepted
- [x] **cli-plan-apply:** renovatio plan seguido de apply --no-dry-run --out sobre un fixture soportado escribe Java traducido, sin TODO, y el contenido funcional coincide con la generación directa.; stages: specified, planned, implemented, verified, accepted
- [x] **api-lifecycle:** La integración API/proveedor de plan y apply verifica que el resultado incluye lógica COBOL traducida y archivos generados, no sólo el ciclo de persistencia.; stages: specified, planned, implemented, verified, accepted
- [x] **manual-actions:** El camino canónico conserva un reporte único de ManualActionItem y no oculta fallos de enriquecimiento como una generación exitosa con TODO.; stages: specified, planned, implemented, verified, accepted
- [x] **architecture-record:** Un ADR identifica la cadena canónica, la responsabilidad de JavaGenerationService/TargetEmitter/OpenRewrite y los límites legacy/deprecados.; stages: specified, planned, implemented, verified, accepted
- [x] **regression-green:** Los módulos provider-cobol, CLI, API, OpenRewrite y el harness de caracterización quedan verdes.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- architecture-decision-record
- verification-report
