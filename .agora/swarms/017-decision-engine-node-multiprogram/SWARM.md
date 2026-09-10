---
schema: "agora/swarm/v1"
id: "decision-engine-node-multiprogram"
method: "spec-driven"
status: "running"
branch: "fix/f8-review-findings"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm decision-engine-node-multiprogram

## Objective

Make Node target generation deterministic and successful for multi-program COBOL workspaces by emitting program-specific source artifacts once per program and compatible shared application files once per project, without changing Java output or semantic IR.

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
