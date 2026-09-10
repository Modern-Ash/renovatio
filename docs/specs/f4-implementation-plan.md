# F4 PersistenceStrategy — Implementation Plan

- **Work item:** `decision-engine-f4/f4-persistence-strategy`
- **Date:** 2026-09-01
- **Spec:** `docs/specs/f4-persistence-strategy.md`

## Phase 1: Module + Classifier (TDD)

### 1.1 Create `renovatio-persistence` Maven module

- New module under root `pom.xml`
- Dependencies: `renovatio-semantic-ir`, `renovatio-profile`, `renovatio-shared`
- ArchUnit rules: no OpenRewrite, JavaPoet, Spring MVC, template, or target-language compiler dependency

### 1.2 Implement `DataAccessKind` enum

```java
public enum DataAccessKind {
    VSAM_KEY, VSAM_SEQUENTIAL, SEQUENTIAL_FD, EXEC_SQL,
    FLAT_FILE_REDEFINES, RESIDUAL
}
```

### 1.3 Implement classification records

- `DataAccessClassification` record (id, sourceId, kind, resourceReference, keyShape, recordShape, discriminator, confidence, evidenceIds, provenance)
- `KeyShape` record (fields: list, NONE)
- `RecordShape` record (fdName, table, columns, UNKNOWN)
- `DiscriminatorValue` record (flag, layout)

### 1.4 Implement `DataAccessClassifier`

- TDD: write failing tests for each `DataAccessKind` using F2 `SemanticProgram` fixtures
- `IoKind.DATABASE` → `EXEC_SQL`
- `IoKind.FILE` + `READ_BY_KEY` → `VSAM_KEY`
- `IoKind.FILE` + `READ_ALL`/`READ_SEQUENTIAL` → `VSAM_SEQUENTIAL`
- `IoKind.FILE` + known FD without REDEFINES → `SEQUENTIAL_FD`
- `IoKind.FILE` + known FD with REDEFINES → `FLAT_FILE_REDEFINES`
- `UNCLASSIFIED_DATA_ACCESS` → `RESIDUAL`

### 1.5 Stable identity

- SHA-256 over `persistence-classification.v1` + programId + resourceReference + kind + span
- Deterministic ordering by id

## Phase 2: SPI + Strategies (TDD)

### 2.1 Define `PersistenceStrategy` interface

```java
public interface PersistenceStrategy {
    boolean supports(DataAccessClassification classification, MigrationProfile.Language target);
    PersistenceArtifacts emit(DataAccessClassification classification, TargetModel targetModel, MigrationProfiles.EffectiveProfile profile);
}
```

### 2.2 Define `PersistenceArtifacts` record

- entityId, entitySource, repositoryId, repositorySource, configSnippet, diagnostics

### 2.3 Implement `InMemoryStrategy`

- TDD: tests for Map-backed stub class + InMemoryRepository
- Ignores transaction boundary

### 2.4 Implement `JpaStrategy`

- TDD: tests for @Entity + @Id + @Transactional generation
- Delegates EXEC_SQL to existing `Db2MigrationService`
- For file accesses: generates entity from FD layout
- Transaction boundary from profile

### 2.5 Implement `SpringDataJdbcStrategy`

- TDD: tests for CrudRepository instead of JpaRepository
- Config snippet for spring.data.jdbc.repositories.type

### 2.6 Implement `PersistenceStrategyRegistry`

- Resolve by kind + target
- Default fallback from profile
- Per-source override from `sourceStrategies` map

## Phase 3: Profile Integration

### 3.1 Extend `MigrationProfile.Persistence`

- Add `Map<String, String> sourceStrategies` field (classification id → strategy name)

### 3.2 Update `MigrationProfiles.effective()`

- Merge `sourceStrategies` during profile resolution
- Validate strategy names and `supports()` compatibility

## Phase 4: REST API

### 4.1 Add `GET /api/projects/{id}/data-accesses`

- Controller in `renovatio-api`
- Returns classifications with confidence and suggested strategy
- 404 for unknown project, 422 for analysis not run

### 4.2 Contract tests

- Test response schema, filtering, empty state

## Phase 5: Wizard Step

### 5.1 Add "Persistence" step after "Decisions"

- Table of classified data accesses per program
- Per-source strategy dropdown
- Expandable entity/repository preview
- Override entity name and key field

### 5.2 Component tests

- Loading, strategy change, confidence display, empty state, validation

## Phase 6: Orchestration + CLI

### 6.1 Integrate into emission pipeline

- Classifier before emitter
- Aggregate persistence artifacts into `EmittedArtifacts`
- Path collision detection

### 6.2 Add CLI command `renovatio persistence <project>`

- List classified accesses with kind, confidence, suggested strategy

## Phase 7: Verification

### 7.1 Issue-#122 characterization

- Default profile with no sourceStrategies → byte-identical output

### 7.2 Maven reactor

- `mvn clean install` green
- ArchUnit isolation tests
- JaCoCo thresholds unchanged

## Commit strategy

1. Module skeleton + ArchUnit
2. Classification records + classifier (TDD)
3. SPI interface + artifacts record
4. InMemoryStrategy (TDD)
5. JpaStrategy (TDD)
6. SpringDataJdbcStrategy (TDD)
7. Registry (TDD)
8. Profile extension
9. REST API endpoint
10. Wizard step
11. Orchestration integration
12. CLI command
13. Verification + regression
