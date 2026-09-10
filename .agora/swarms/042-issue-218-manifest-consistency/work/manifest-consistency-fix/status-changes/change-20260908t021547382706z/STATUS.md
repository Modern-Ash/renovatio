---
schema: "agora/status-change/v1"
id: "change-20260908t021547382706z"
subject-type: "work"
subject: "issue-218-manifest-consistency/manifest-consistency-fix"
action: "work.cancel"
previous-status: "active"
target-status: "cancelled"
actor: "project:owner"
sequence: 1
created-at: "2026-09-08T02:15:47.382763Z"
---

# Status change change-20260908t021547382706z

## Reason

No hay defecto. JavaGenerationRegistryRoutingTest (13/13) y todo renovatio-provider-cobol (114/114) pasan en main (5835fdeb) con el reactor local reconstruido. Las 2 fallas observadas al abrir el issue fueron artefactos ~/.m2 stale de un 'mvn install' previo de la rama agora/renovatio-workbench-bootstrap durante el analisis de estado; no reproducen con jars de main. Ver comentario en el issue.
