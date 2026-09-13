---
schema: "agora/implementation-plan/v1"
work-item: "canonical-domain-architecture-model"
swarm: "architecture-convergence-2026"
revision: 1
status: "implemented"
---

# Implementation Plan: Canonical Domain-Architecture Model Unification

## Phase 1: Foundation (Week 1)

### 1.1 Schema Definition
- [x] Create `ArchitectureModel` record with schema versioning
- [x] Preserve the supported `DomainModel` v1 contract and reject unknown schemas actionably
- [x] Create `DecisionSet` record
- [x] Define `CanonicalProjection` aggregate

### 1.2 ArchUnit Rules
- [x] Add ArchUnit rules for domain/architecture/manifest forbidden dependencies

### 1.3 Property Tests
- [x] Identity determinism and ordering stability
- [x] Hash integrity: content changes → hash changes
- [x] Provenance traceability via source content hashes

## Phase 2: Projection (Week 2)

### 2.1 ArchitectureProjector (New)
- [x] Create `ArchitectureProjector` and project DomainModel + DecisionSet
- [x] Make the existing transformer expose the canonical projection on its sole result
- [x] Preserve transformer contract tests during migration

### 2.2 ArtifactPlanner (Refactor)
- [x] Bind the manifest hash to ArchitectureModel hash and canonical artifact ordering

### 2.3 CanonicalProjection
- [x] Create `CanonicalProjectionService`
- [x] Return one content-addressed projection in `ArchitectureResult` for preview/apply

## Phase 3: Migration (Week 3)

### 3.1 Schema Adapters
- [x] Keep DomainModel v1 compatible; reject unsupported versions with supported version in message
- [x] Derive the legacy ArchitectureGraph view from ArchitectureModel

### 3.2 Consumer Migration
- [ ] Migrate `WorkbenchDomainModelService` to use `CanonicalProjection`
- [ ] Migrate `WorkbenchArchitectureCanvasService` to use `ArchitectureModel`
- [x] Preview/apply use the same `prepareArchitecture` result and manifest hash
- [ ] Update API DTOs to reflect new structure

### 3.3 Legacy Encapsulation
- [x] Mark `ArchitectureTransformer` as `@Deprecated(forRemoval=true)`
- [x] Make `ArchitectureGraph` a compatibility view derived from `ArchitectureModel`
- [x] Document compatibility and retirement below

## Phase 4: Verification (Week 4)

### 4.1 Contract Tests
- [x] Preview/apply share the canonical `ArchitectureResult`
- [x] Existing DomainModel v1 remains accepted; unknown versions fail actionably
- [x] No second projection or dual-write path was introduced

### 4.2 Property Tests
- [x] Critical invariants and schema compatibility automated

### 4.3 Documentation
- [x] Update specification, compatibility report and ADR

## Risk Mitigation

1. **Incremental migration**: Each phase can be merged independently
2. **Backward compatibility**: Adapters ensure existing code continues to work
3. **Rollback plan**: Keep deprecated classes until all consumers migrated
4. **Test coverage**: Each phase has corresponding tests

## Success Criteria

- [x] Single projection result for preview and apply
- [x] Level ownership documented
- [x] Invariants enforced by constructors and tests
- [x] Existing projects remain compatible or fail clearly
- [x] No dual write in any code path
