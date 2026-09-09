---
schema: "agora/work/v1"
id: "canonical-domain-architecture-model"
swarm: "architecture-convergence-2026"
title: "AC-04: Unificar DomainModel, ArchitectureModel y ArtifactManifest"
state: "implementing"
revision: 2
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"semantic-levels":"Un ADR define qu\u00e9 datos pertenecen a Source IR, Semantic IR, Domain Model, Decision Set, Architecture Model y Artifact Manifest, sin campos duplicados de autoridad.","canonical-projection":"Existe una sola proyecci\u00f3n productiva DomainModel + DecisionSet -> ArchitectureModel + ArtifactManifest.","preview-apply":"Preview y apply consumen el mismo manifest identificado por hash; no vuelven a proyectar por caminos distintos.","compatibility":"Los formatos persistidos existentes se migran mediante versi\u00f3n/adapters expl\u00edcitos o fallan con diagn\u00f3stico accionable; no hay dual write permanente.","invariants":"Identidad, provenance, orden estable, hashes, unknown nodes y action items tienen invariantes automatizados.","dependency-rules":"Tests ArchUnit o equivalentes impiden dependencias de dominio hacia Spring, filesystem, LLM o targets.","legacy-retirement":"ArchitectureTransformer, ArchitectureGraph y ArchitectureModelAdapter quedan fusionados, encapsulados como detalle interno o marcados con fecha y prueba de retiro inequ\u00edvocas."}
satisfied-criteria: []
criterion-statuses: {"semantic-levels":["specified","planned","implemented"],"canonical-projection":["specified","planned","implemented"],"preview-apply":["specified","planned","implemented"],"compatibility":["specified","planned","implemented"],"invariants":["specified","planned","implemented"],"dependency-rules":["specified","planned","implemented"],"legacy-retirement":["specified","planned","implemented"]}
required-artifacts: ["spec","implementation-plan","adr-set","architecture-diagram","compatibility-report","test-report"]
child-work-refs: []
budget-limits: null
---

# AC-04: Unificar DomainModel, ArchitectureModel y ArtifactManifest

## Description

No description provided.

## Acceptance criteria

- [ ] **semantic-levels:** Un ADR define qué datos pertenecen a Source IR, Semantic IR, Domain Model, Decision Set, Architecture Model y Artifact Manifest, sin campos duplicados de autoridad.; stages: specified, planned, implemented
- [ ] **canonical-projection:** Existe una sola proyección productiva DomainModel + DecisionSet -> ArchitectureModel + ArtifactManifest.; stages: specified, planned, implemented
- [ ] **preview-apply:** Preview y apply consumen el mismo manifest identificado por hash; no vuelven a proyectar por caminos distintos.; stages: specified, planned, implemented
- [ ] **compatibility:** Los formatos persistidos existentes se migran mediante versión/adapters explícitos o fallan con diagnóstico accionable; no hay dual write permanente.; stages: specified, planned, implemented
- [ ] **invariants:** Identidad, provenance, orden estable, hashes, unknown nodes y action items tienen invariantes automatizados.; stages: specified, planned, implemented
- [ ] **dependency-rules:** Tests ArchUnit o equivalentes impiden dependencias de dominio hacia Spring, filesystem, LLM o targets.; stages: specified, planned, implemented
- [ ] **legacy-retirement:** ArchitectureTransformer, ArchitectureGraph y ArchitectureModelAdapter quedan fusionados, encapsulados como detalle interno o marcados con fecha y prueba de retiro inequívocas.; stages: specified, planned, implemented

## Required artifacts

- spec
- implementation-plan
- adr-set
- architecture-diagram
- compatibility-report
- test-report
