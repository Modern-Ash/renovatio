---
schema: "agora/work/v1"
id: "repository-community-governance"
swarm: "architecture-convergence-2026"
title: "AC-03: Higiene del repositorio y fundamentos open source"
state: "completed"
revision: 1
operational-status: "active"
status-reason: "Owner approved MIT License on 2026-09-11; LICENSE can now be added and the license criterion can proceed."
status-by: "project:agent"
status-at: "2026-09-11T19:09:32.987817Z"
acceptance-criteria: {"license":"El titular aprueba y se agrega una licencia expl\u00edcita compatible con dependencias y modelo de contribuci\u00f3n.","community-files":"Existen CONTRIBUTING.md, CODE_OF_CONDUCT.md, SECURITY.md, CHANGELOG.md y pol\u00edtica DCO o CLA claramente elegida.","tracked-artifacts":"data/renovatio-db.mv.db y renovatio-provider-cobol/target_bad dejan de estar versionados y se agregan reglas preventivas.","agora-retention":"Existe una pol\u00edtica aprobada para conservar \u00edndices, decisiones y evidencia de releases en Git y externalizar o archivar ejecuciones voluminosas.","history-scan":"Se revisa todo el historial Git por secretos, PII, bases de datos y artifacts sensibles; cada hallazgo tiene remediaci\u00f3n o aceptaci\u00f3n expl\u00edcita.","dependency-license-scan":"Se registra un inventario de licencias de dependencias y no quedan incompatibilidades sin decisi\u00f3n humana.","repo-size":"Se mide tama\u00f1o antes/despu\u00e9s y se documenta cualquier reescritura de historia, que requiere aprobaci\u00f3n expl\u00edcita."}
satisfied-criteria: ["license","community-files","tracked-artifacts","agora-retention","history-scan","dependency-license-scan","repo-size"]
criterion-statuses: {"license":["specified","planned","implemented","verified","accepted"],"community-files":["specified","planned","implemented","verified","accepted"],"tracked-artifacts":["specified","planned","implemented","verified","accepted"],"agora-retention":["specified","planned","implemented","verified","accepted"],"history-scan":["specified","planned","implemented","verified","accepted"],"dependency-license-scan":["specified","planned","implemented","verified","accepted"],"repo-size":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","license-review","repository-hygiene-report","security-report"]
child-work-refs: []
budget-limits: null
---

# AC-03: Higiene del repositorio y fundamentos open source

## Description

Eliminar artefactos accidentales del tree, definir política de evidencia Agora y agregar archivos legales/comunitarios para preparación open source, sin elegir licencia ni reescribir historia sin aprobación humana explícita.

## Acceptance criteria

- [x] **license:** El titular aprueba y se agrega una licencia explícita compatible con dependencias y modelo de contribución.; stages: specified, planned, implemented, verified, accepted
- [x] **community-files:** Existen CONTRIBUTING.md, CODE_OF_CONDUCT.md, SECURITY.md, CHANGELOG.md y política DCO o CLA claramente elegida.; stages: specified, planned, implemented, verified, accepted
- [x] **tracked-artifacts:** data/renovatio-db.mv.db y renovatio-provider-cobol/target_bad dejan de estar versionados y se agregan reglas preventivas.; stages: specified, planned, implemented, verified, accepted
- [x] **agora-retention:** Existe una política aprobada para conservar índices, decisiones y evidencia de releases en Git y externalizar o archivar ejecuciones voluminosas.; stages: specified, planned, implemented, verified, accepted
- [x] **history-scan:** Se revisa todo el historial Git por secretos, PII, bases de datos y artifacts sensibles; cada hallazgo tiene remediación o aceptación explícita.; stages: specified, planned, implemented, verified, accepted
- [x] **dependency-license-scan:** Se registra un inventario de licencias de dependencias y no quedan incompatibilidades sin decisión humana.; stages: specified, planned, implemented, verified, accepted
- [x] **repo-size:** Se mide tamaño antes/después y se documenta cualquier reescritura de historia, que requiere aprobación explícita.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- license-review
- repository-hygiene-report
- security-report
