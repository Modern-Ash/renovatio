---
schema: "agora/swarm/v1"
id: "issue-207-perform-thru-varying-times"
method: "spec-driven"
status: "running"
branch: "agora/issue-207-perform-thru-varying-times"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm issue-207-perform-thru-varying-times

## Objective

Traducir PERFORM en todas sus variantes (THRU / VARYING / UNTIL / TIMES) con semántica correcta y extraer cada párrafo referenciado a un método Java privado, eliminando la duplicación por inlineado (#207).

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
