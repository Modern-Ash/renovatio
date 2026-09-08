---
schema: "agora/swarm/v1"
id: "decision-engine-f4-dataaccess"
method: "spec-driven"
status: "completed"
branch: "agora/decision-engine-f8"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm decision-engine-f4-dataaccess

## Objective

Cerrar el gap de F4: hacer que GET /api/projects/{id}/data-accesses cargue y devuelva las clasificaciones reales del análisis del proyecto, manteniendo determinismo, aislamiento por proyecto y compatibilidad con el contrato existente.

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
