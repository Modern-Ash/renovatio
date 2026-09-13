---
schema: "agora/adr/v1"
id: "ADR-001-semantic-levels"
work-item: "canonical-domain-architecture-model"
swarm: "architecture-convergence-2026"
status: "accepted"
decision-date: "2026-09-09"
---

# ADR-001: Semantic Levels for Renovatio

## Status

Accepted

## Context

Renovatio processes source code through multiple transformation stages. Currently, there are two overlapping paths of authority:

1. **Direct path**: `SemanticProgram → ArchitectureTransformer → ArchitectureGraph + ArtifactManifest`
2. **Domain path**: `SemanticProgram → DomainModel → ArchitectureModel → adapter → ArchitectureGraph`

This dual authority violates the principle of single source of truth and can lead to inconsistent previews and generated artifacts.

## Decision

Define 6 semantic levels with clear ownership and no duplicate fields:

### Level Definitions

| Level | Schema | Input | Output | Owner | Description |
|-------|--------|-------|--------|-------|-------------|
| **Source IR** | `SemanticProgram` | Raw source code | Parsed program | Language Provider | Language-specific parsing and initial analysis |
| **Semantic IR** | `SemanticProgram` | `SemanticProgram` | Enriched program | Semantic Passes | Cross-program analysis, type resolution, data flow |
| **Domain Model** | `DomainModel` | `SemanticProgram` | Business model | `SemanticDomainProjector` | Target-independent business concepts and relationships |
| **Decision Set** | `DecisionSet` | `DomainModel` | Architectural decisions | Decision Engine | Technology choices, patterns, and constraints |
| **Architecture Model** | `ArchitectureModel` | `DomainModel + DecisionSet` | Technical architecture | `ArchitectureProjector` | Modules, components, and their relationships |
| **Artifact Manifest** | `ArtifactManifest` | `ArchitectureModel` | File layout | `ArtifactPlanner` | Generated file paths and metadata |

### Field Ownership Rules

1. **No duplicate fields**: Each field exists in exactly one level.
2. **Immutable progression**: Data flows forward only; no back-references.
3. **Content-addressed**: Each level produces a content hash for integrity verification.

### Key Type Definitions

```java
// Domain Model - Business concepts
public record DomainModel(
    String schemaVersion,
    String projectId,
    List<DomainNode> nodes,
    List<DomainRelation> relations,
    List<BusinessInvariant> invariants
) {}

// Architecture Model - Technical structure
public record ArchitectureModel(
    String schemaVersion,
    String requestHash,
    String domainModelHash,
    String decisionSetHash,
    List<Module> modules,
    List<Component> components,
    List<Relation> relations
) {}

// Artifact Manifest - File layout
public record ArtifactManifest(
    String schemaVersion,
    String architectureModelHash,
    List<Artifact> artifacts
) {}
```

## Consequences

### Positive
- Single source of truth for each concern
- Clear ownership and responsibility boundaries
- Deterministic previews and generations
- Easier testing and validation

### Negative
- Migration effort for existing code
- Need for compatibility adapters during transition
- Additional abstraction layer

## Compliance

- All existing consumers must be migrated incrementally
- Schema versioning ensures backward compatibility
- ArchUnit rules enforce dependency boundaries
