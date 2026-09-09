---
schema: "agora/spec/v1"
work-item: "canonical-domain-architecture-model"
swarm: "architecture-convergence-2026"
revision: 1
status: "draft"
---

# Specification: Canonical Domain-Architecture Model Unification

## Problem Statement

Conviven dos caminos de autoridad para la transformación de código fuente a artefactos generados:

1. **Camino directo**: `SemanticProgram → ArchitectureTransformer → ArchitectureGraph + ArtifactManifest`
2. **Camino Domain**: `SemanticProgram → DomainModel → ArchitectureModel → adapter → ArchitectureGraph`

Preview, persistencia y generación pueden describir soluciones distintas para el mismo snapshot, violando el principio de una sola fuente de verdad.

## Proposed Solution

### Semantic Levels (ADR)

Definir 6 niveles semánticos claros con campos no duplicados:

| Level | Input | Output | Owner |
|-------|-------|--------|-------|
| **Source IR** | Raw source code | `SemanticProgram` (parsed) | Language Provider |
| **Semantic IR** | `SemanticProgram` | `SemanticProgram` (enriched) | Semantic Passes |
| **Domain Model** | `SemanticProgram` | `DomainModel` | `SemanticDomainProjector` |
| **Decision Set** | `DomainModel` | `DecisionSet` | Decision Engine |
| **Architecture Model** | `DomainModel + DecisionSet` | `ArchitectureModel` | `ArchitectureProjector` (new) |
| **Artifact Manifest** | `ArchitectureModel` | `ArtifactManifest` | `ArtifactPlanner` |

### Unified Projection

```java
// Single productive projection
public record CanonicalProjection(
    DomainModel domain,
    DecisionSet decisions,
    ArchitectureModel architecture,
    ArtifactManifest manifest,
    String manifestHash  // content-addressed for preview/apply sharing
) {}
```

### Schema Versioning

```java
public record DomainModel(String schemaVersion, ...) {
    public static final String SCHEMA_VERSION = "2"; // bumped for unification
}

public record ArchitectureModel(String schemaVersion, ...) {
    public static final String SCHEMA_VERSION = "1";
}
```

### Compatibility Adapters

```java
public interface SchemaAdapter<Old, New> {
    New migrate(Old old);
    String sourceVersion();
    String targetVersion();
}
```

## Invariants

1. **Identity**: Each node/component/artifact has a deterministic ID derived from its content and context.
2. **Provenance**: Every element traces back to its source with content SHA-256.
3. **Stable ordering**: Collections are sorted by ID for deterministic serialization.
4. **Hash integrity**: `manifestHash` is computed from canonical serialization of the manifest.
5. **Unknown nodes**: References to non-existent nodes fail fast with clear diagnostics.
6. **Action items**: Missing data produces actionable diagnostics, not silent degradation.

## Dependency Rules (ArchUnit)

```
domain.model -> no Spring, no filesystem, no LLM, no targets
architecture -> no Spring, no filesystem, no LLM
artifact.manifest -> no Spring, no filesystem
```

## Evidence Requirements

- Golden serialization of versioned schemas
- Property tests for ordering, identity, provenance, and hash
- Contract test for preview/apply with same manifest
- ArchUnit without forbidden dependencies
- Compatibility fixtures for existing persisted projects
