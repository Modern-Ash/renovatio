---
schema: "agora/swarm/v1"
id: "issue-226-application-pipeline"
method: "spec-driven"
status: "running"
branch: "feature/ac-05-application-pipeline"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm issue-226-application-pipeline

## Objective

Implementar el issue #226 mediante una única capa application para plan, preview, validate y apply, con manifest compartido, precondiciones de snapshot, side effects detrás de puertos, idempotencia, atomicidad y adapters finos; excluir reescrituras de parsers, emitters y lógica específica de transports o targets.

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
