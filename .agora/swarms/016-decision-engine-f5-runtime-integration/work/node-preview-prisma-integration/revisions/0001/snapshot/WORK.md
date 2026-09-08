---
schema: "agora/work/v1"
id: "node-preview-prisma-integration"
swarm: "decision-engine-f5-runtime-integration"
title: "Integrar preview Node real y artefactos Prisma al pipeline"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"project-backed-preview":"El preview Node resuelve el proyecto real, analiza sus programas COBOL y emite artefactos por programa; no usa un SemanticProgram placeholder.","prisma-artifacts":"Cuando el perfil efectivo selecciona Prisma, la emisi\u00f3n Node produce artefactos Prisma deterministas y no omite silenciosamente la estrategia.","idiom-integration":"El pipeline productivo consulta NodeIdiomCatalog y expone acciones manuales para patrones no soportados.","multi-program-compatibility":"La integraci\u00f3n preserva deduplicaci\u00f3n de artefactos compartidos, colisiones seguras y salidas deterministas para m\u00faltiples programas.","regression-quality":"Existen pruebas focalizadas, compilaci\u00f3n Maven relevante y diff-check sin regresiones."}
satisfied-criteria: ["project-backed-preview","prisma-artifacts","idiom-integration","multi-program-compatibility","regression-quality"]
criterion-statuses: {"project-backed-preview":["specified","planned","implemented","verified","accepted"],"prisma-artifacts":["specified","planned","implemented","verified","accepted"],"idiom-integration":["specified","planned","implemented","verified","accepted"],"multi-program-compatibility":["specified","planned","implemented","verified","accepted"],"regression-quality":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Integrar preview Node real y artefactos Prisma al pipeline

## Description

Cerrar los gaps observados en F5: eliminar el programa sintético del preview Node, resolver proyecto/workspace y perfil efectivo, conectar estrategias Prisma/TypeORM y el catálogo de idioms al renderer, y conservar compatibilidad multi-programa y determinismo.

## Acceptance criteria

- [x] **project-backed-preview:** El preview Node resuelve el proyecto real, analiza sus programas COBOL y emite artefactos por programa; no usa un SemanticProgram placeholder.; stages: specified, planned, implemented, verified, accepted
- [x] **prisma-artifacts:** Cuando el perfil efectivo selecciona Prisma, la emisión Node produce artefactos Prisma deterministas y no omite silenciosamente la estrategia.; stages: specified, planned, implemented, verified, accepted
- [x] **idiom-integration:** El pipeline productivo consulta NodeIdiomCatalog y expone acciones manuales para patrones no soportados.; stages: specified, planned, implemented, verified, accepted
- [x] **multi-program-compatibility:** La integración preserva deduplicación de artefactos compartidos, colisiones seguras y salidas deterministas para múltiples programas.; stages: specified, planned, implemented, verified, accepted
- [x] **regression-quality:** Existen pruebas focalizadas, compilación Maven relevante y diff-check sin regresiones.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- verification-report
- review-report
