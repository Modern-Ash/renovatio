---
schema: "agora/swarm/v1"
id: "decision-engine-f2"
method: "spec-driven"
status: "completed"
branch: "agora/f2-semantic-ir-emitter"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm decision-engine-f2

## Objective

Deliver issue #147 by introducing a target-neutral renovatio-semantic-ir and TargetEmitter SPI/registry between COBOL analysis and emission, projecting existing Java annotations from the neutral model while preserving byte-identical Java output. Exclude real Node/Python emitters, architecture transforms, fine persistence classification, and observable behavior changes; complete only with architectural isolation, characterization, Maven, MCP, and CLI evidence.

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
