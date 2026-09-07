---
schema: "agora/swarm/v1"
id: "renovatio-workbench-identity"
method: "spec-driven"
status: "ready"
branch: "agora/renovatio-workbench"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm renovatio-workbench-identity

## Objective

Define Renovatio Workbench identity, login, session propagation and authorization boundaries for live project access and future write operations; this cycle is intentionally deferred from implementation.

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
