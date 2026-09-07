---
schema: "agora/swarm/v1"
id: "renovatio-sqlite-migration"
method: "spec-driven"
status: "completed"
branch: "agora/renovatio-workbench"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm renovatio-sqlite-migration

## Objective

Migrate the existing local Renovatio development data from H2 to SQLite while preserving source data, validating row counts and retaining H2 as an untouched rollback artifact.

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
