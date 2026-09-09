---
schema: "agora/implementation-plan/v1"
work-item: "canonical-domain-architecture-model"
swarm: "architecture-convergence-2026"
revision: 1
status: "draft"
---

# Implementation Plan: Canonical Domain-Architecture Model Unification

## Phase 1: Foundation (Week 1)

### 1.1 Schema Definition
- [ ] Create `ArchitectureModel` record with schema versioning
- [ ] Update `DomainModel` to schema version 2
- [ ] Create `DecisionSet` record (if not exists)
- [ ] Define `CanonicalProjection` aggregate

### 1.2 ArchUnit Rules
- [ ] Create `DomainModelArchitectureTest` - no Spring, filesystem, LLM
- [ ] Create `ArchitectureModelArchitectureTest` - no Spring, filesystem
- [ ] Create `ArtifactManifestArchitectureTest` - no Spring, filesystem
- [ ] Add rules to existing `ArchitectureContractsTest`

### 1.3 Property Tests
- [ ] Identity determinism: same input → same IDs
- [ ] Ordering stability: collections sorted by ID
- [ ] Hash integrity: content changes → hash changes
- [ ] Provenance traceability: every element has source reference

## Phase 2: Projection (Week 2)

### 2.1 ArchitectureProjector (New)
- [ ] Create `ArchitectureProjector` class
- [ ] Implement `project(DomainModel, DecisionSet) → ArchitectureModel`
- [ ] Migrate logic from `ArchitectureTransformer.transform()`
- [ ] Add contract tests with existing `ArchitectureTransformerTest`

### 2.2 ArtifactPlanner (Refactor)
- [ ] Refactor `ArtifactLayoutPlanner` to consume `ArchitectureModel`
- [ ] Update `ArtifactManifest` to include `architectureModelHash`
- [ ] Add hash-based deduplication for preview/apply sharing

### 2.3 CanonicalProjection
- [ ] Create `CanonicalProjectionService`
- [ ] Implement single projection path: `DomainModel + DecisionSet → ArchitectureModel + ArtifactManifest`
- [ ] Add content-addressed caching for manifests

## Phase 3: Migration (Week 3)

### 3.1 Schema Adapters
- [ ] Create `DomainModelV1ToV2Adapter`
- [ ] Create `ArchitectureGraphToModelAdapter` (for backward compatibility)
- [ ] Add version detection and auto-migration

### 3.2 Consumer Migration
- [ ] Migrate `WorkbenchDomainModelService` to use `CanonicalProjection`
- [ ] Migrate `WorkbenchArchitectureCanvasService` to use `ArchitectureModel`
- [ ] Migrate preview/apply to share manifest hash
- [ ] Update API DTOs to reflect new structure

### 3.3 Legacy Encapsulation
- [ ] Mark `ArchitectureTransformer` as `@Deprecated(forRemoval=true)`
- [ ] Encapsulate `ArchitectureGraph` as internal detail of `ArchitectureModel`
- [ ] Add migration guide to documentation

## Phase 4: Verification (Week 4)

### 4.1 Contract Tests
- [ ] Preview/apply share same manifest hash
- [ ] Existing persisted projects migrate or fail with diagnostics
- [ ] No dual write in any code path

### 4.2 Property Tests
- [ ] All invariants automated
- [ ] Golden serialization of versioned schemas
- [ ] Compatibility fixtures for existing projects

### 4.3 Documentation
- [ ] Update architecture documentation
- [ ] Add migration guide
- [ ] Update ADR with final decisions

## Risk Mitigation

1. **Incremental migration**: Each phase can be merged independently
2. **Backward compatibility**: Adapters ensure existing code continues to work
3. **Rollback plan**: Keep deprecated classes until all consumers migrated
4. **Test coverage**: Each phase has corresponding tests

## Success Criteria

- [ ] Single projection path for preview and apply
- [ ] No duplicate fields across levels
- [ ] All invariants enforced by tests
- [ ] Existing projects migrate or fail with clear diagnostics
- [ ] No dual write in any code path
