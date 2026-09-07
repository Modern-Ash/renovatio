---
schema: "agora/swarm/v1"
id: "agora-ledger-validation-repair"
method: "spec-driven"
status: "running"
branch: "agora/restore-ledger-validation"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm agora-ledger-validation-repair

## Objective

Restore repository-wide Agora ledger validation for GitHub issue #188 without changing runtime behavior.

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
