---
schema: "agora/work/v1"
id: "canonical-domain-architecture-model"
swarm: "architecture-convergence-2026"
title: "AC-04: Unificar DomainModel, ArchitectureModel y ArtifactManifest"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: "Spec drafting in progress"
status-by: "project:agent"
acceptance-criteria: {"semantic-levels":"Un ADR define qué datos pertenecen a Source IR, Semantic IR, Domain Model, Decision Set, Architecture Model y Artifact Manifest, sin campos duplicados de autoridad.","canonical-projection":"Existe una sola proyección productiva DomainModel + DecisionSet -> ArchitectureModel + ArtifactManifest.","preview-apply":"Preview y apply consumen el mismo manifest identificado por hash; no vuelven a proyectar por caminos distintos.","compatibility":"Los formatos persistidos existentes se migran mediante versión/adapters explícitos o fallan con diagnóstico accionable; no hay dual write permanente.","invariants":"Identidad, provenance, orden estable, hashes, unknown nodes y action items tienen invariantes automatizados.","dependency-rules":"Tests ArchUnit o equivalentes impiden dependencias de dominio hacia Spring, filesystem, LLM o targets.","legacy-retirement":"ArchitectureTransformer, ArchitectureGraph y ArchitectureModelAdapter quedan fusionados, encapsulados como detalle interno o marcados con fecha y prueba de retiro inequívocas."}
satisfied-criteria: []
criterion-statuses: {"semantic-levels":["specified"],"canonical-projection":[],"preview-apply":[],"compatibility":[],"invariants":[],"dependency-rules":[],"legacy-retirement":[]}
required-artifacts: ["spec","implementation-plan","adr-set","architecture-diagram","compatibility-report","test-report"]
---

# Canonical Domain-Architecture Model Unification

## Acceptance Criteria

- [ ] **`semantic-levels`** — Un ADR define qué datos pertenecen a Source IR, Semantic IR, Domain Model, Decision Set, Architecture Model y Artifact Manifest, sin campos duplicados de autoridad.
- [ ] **`canonical-projection`** — Existe una sola proyección productiva DomainModel + DecisionSet -> ArchitectureModel + ArtifactManifest.
- [ ] **`preview-apply`** — Preview y apply consumen el mismo manifest identificado por hash; no vuelven a proyectar por caminos distintos.
- [ ] **`compatibility`** — Los formatos persistidos existentes se migran mediante versión/adapters explícitos o fallan con diagnóstico accionable; no hay dual write permanente.
- [ ] **`invariants`** — Identidad, provenance, orden estable, hashes, unknown nodes y action items tienen invariantes automatizados.
- [ ] **`dependency-rules`** — Tests ArchUnit o equivalentes impiden dependencias de dominio hacia Spring, filesystem, LLM o targets.
- [ ] **`legacy-retirement`** — ArchitectureTransformer, ArchitectureGraph y ArchitectureModelAdapter quedan fusionados, encapsulados como detalle interno o marcados con fecha y prueba de retiro inequívocas.

## Out of Scope

- No cambiar UI ni agregar targets.
- No mantener dual write permanente.
- No trasladar tipos de Spring, filesystem o proveedor LLM al dominio.

## Dependencies

- Depende de AC-01 (baseline reconciliation).
- Bloquea AC-05 (unified pipeline).
- Coordina con AC-08, AC-12 y AC-13 para preservar fronteras.
