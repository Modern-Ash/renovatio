---
schema: "agora/swarm/v1"
id: "issue-223-reproducible-build-ci"
method: "spec-driven"
status: "completed"
branch: "agora/issue-223-reproducible-build-ci"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm issue-223-reproducible-build-ci

## Objective

Fix issue #223 (AC-02): Convertir el repositorio en un build reproducible desde clone limpio para Java, UI, Workbench y Python, y proteger main con gates equivalentes. Definir toolchains soportados y una orden bootstrap única, fijar Maven Wrapper y versiones de plugins, resolver colisión Node multiprograma, integrar npm ci antes de Vite, añadir jobs CI con caches y hacer obligatorios los checks de main relevantes.

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
