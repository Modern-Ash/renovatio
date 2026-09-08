---
schema: "agora/work/v1"
id: "manifest-consistency-fix"
swarm: "issue-218-manifest-consistency"
title: "Alinear manifiesto de arquitectura entre preview y generaci\u00f3n (#218)"
state: "implementing"
revision: 1
operational-status: "cancelled"
status-reason: "No hay defecto. JavaGenerationRegistryRoutingTest (13/13) y todo renovatio-provider-cobol (114/114) pasan en main (5835fdeb) con el reactor local reconstruido. Las 2 fallas observadas al abrir el issue fueron artefactos ~/.m2 stale de un 'mvn install' previo de la rama agora/renovatio-workbench-bootstrap durante el analisis de estado; no reproducen con jars de main. Ver comentario en el issue."
status-by: "project:owner"
status-at: "2026-09-08T02:15:47.382618Z"
acceptance-criteria: {"cics-controller-parity":"Para un programa con EXEC CICS, el manifiesto de previewArchitecture y el de generateInterfaceStubs contienen exactamente las mismas rutas, incluido el controller CICS, y generateInterfaceStubs no lanza TARGET_MANIFEST_MISMATCH.","canvas-package-roots-parity":"Cuando el canvas de arquitectura define package roots custom, tanto el preview como la generaci\u00f3n emiten rutas bajo esos roots (p.ej. com/acme/domain/...), no el prefijo modules/<modulo>/ por defecto.","regression-green":"mvn -o test -pl renovatio-provider-cobol -Dexec.skip=true pasa 100% (JavaGenerationRegistryRoutingTest incluido) y no se introducen skips.","root-cause-documented":"La causa raiz de la divergencia queda documentada en el verification-report (que componente calculaba distinto el manifiesto y por que)."}
satisfied-criteria: []
criterion-statuses: {"cics-controller-parity":["specified","planned"],"canvas-package-roots-parity":["specified","planned"],"regression-green":["specified","planned"],"root-cause-documented":["specified","planned"]}
required-artifacts: ["spec","implementation-plan","verification-report"]
child-work-refs: []
budget-limits: null
---

# Alinear manifiesto de arquitectura entre preview y generación (#218)

## Description

previewArchitecture() y generateInterfaceStubs() en JavaGenerationService deben producir el mismo conjunto de rutas de artefactos. Cubre: controller CICS planificado y package roots del canvas de arquitectura. Sin regresión en routing ni en el resto de renovatio-provider-cobol.

## Acceptance criteria

- [ ] **cics-controller-parity:** Para un programa con EXEC CICS, el manifiesto de previewArchitecture y el de generateInterfaceStubs contienen exactamente las mismas rutas, incluido el controller CICS, y generateInterfaceStubs no lanza TARGET_MANIFEST_MISMATCH.; stages: specified, planned
- [ ] **canvas-package-roots-parity:** Cuando el canvas de arquitectura define package roots custom, tanto el preview como la generación emiten rutas bajo esos roots (p.ej. com/acme/domain/...), no el prefijo modules/<modulo>/ por defecto.; stages: specified, planned
- [ ] **regression-green:** mvn -o test -pl renovatio-provider-cobol -Dexec.skip=true pasa 100% (JavaGenerationRegistryRoutingTest incluido) y no se introducen skips.; stages: specified, planned
- [ ] **root-cause-documented:** La causa raiz de la divergencia queda documentada en el verification-report (que componente calculaba distinto el manifiesto y por que).; stages: specified, planned

## Required artifacts

- spec
- implementation-plan
- verification-report
