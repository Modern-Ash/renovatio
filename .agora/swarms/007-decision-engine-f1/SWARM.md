---
schema: "agora/swarm/v1"
id: "decision-engine-f1"
method: "spec-driven"
status: "running"
branch: "agora/f1-decision-layer"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm decision-engine-f1

## Objective

Deliver issue #146 (F1): add a versioned MigrationProfile v1, stable decision registry/resolver, bounded LLM decision suggestions, five profile/decision APIs, and Target/Decisions wizard steps while preserving byte-identical default Java generation and excluding new emitters or real architecture transformations.

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
