# F4 PersistenceStrategy Pluggable

- **Work item:** `decision-engine-f4/f4-persistence-strategy`
- **GitHub issue:** #149 (Epic #152)
- **Method:** Agora `spec-driven` with TDD
- **Status:** governed specification
- **Date:** 2026-09-01
- **F3 compatibility baseline:** TBD (F3 merge commit)

## 1. Outcome

F4 classifies every data access detected by the F2 semantic IR and makes
persistence strategy selection pluggable per source. A new
`PersistenceStrategy` SPI allows JPA, Spring Data JDBC, or in-memory test
stubs to be chosen per classified data access, replacing the single global
`persistence.defaultStrategy` from F1. The REST API exposes classified data
accesses and the UI adds a Persistence wizard step where users choose
strategies per source and override proposed data models.

For the default F1 profile, F4 preserves byte-identical Java output. The new
module introduces no real data migration, DDL generation, or target-language
code beyond the existing Java path.

## 2. Binding clarifications

The Spec Owner accepted these answers on 2026-09-01:

| Question | Binding answer |
|---|---|
| Classification boundary | Classification consumes `SemanticProgram.ioOperations` and `SemanticProgram.unclassifiedDataAccesses` from F2. It produces a `DataAccessClassification` per source, covering VSAM key/sequential/browse, sequential FD, EXEC SQL (DB2), and flat-file with REDEFINES discriminator. Unclassified accesses remain in the residual bucket. |
| SPI contract | `PersistenceStrategy.supports(Classification, Language)` is pure and side-effect-free. `PersistenceStrategy.emit(Classification, TargetModel, EffectiveProfile)` returns `PersistenceArtifacts` containing entity/record, repository interface, and configuration. The SPI does not write files. |
| Strategy scope (YAGNI) | Three strategies in F4: `JPA`, `SPRING_DATA_JDBC`, `IN_MEMORY`. MyBatis, jOOQ, R2DBC, and others are explicitly excluded. |
| Global vs per-source | F1 `persistence.defaultStrategy` is preserved as the fallback. Per-source overrides are stored in `MigrationProfile.persistence.sourceStrategies` (Map keyed by data-access source id). The wizard allows choosing per source; the CLI/API can set via profile overlay. |
| Transaction boundary | `profile.persistence.transactionBoundary` (METHOD/PROGRAM/NONE) maps to `@Transactional` for JPA and Spring Data JDBC. IN_MEMORY ignores it. |
| Existing JPA helpers | `Db2MigrationService` in `renovatio-provider-cobol` is wrapped as the JPA strategy for `DATABASE` accesses. Not rewritten; the strategy delegates to it. |

## 3. Scope and non-goals

F4 delivers:

1. a new `renovatio-persistence` Maven module with the `PersistenceStrategy`
   SPI and classifier;
2. a `DataAccessClassifier` that consumes F2 semantic IR and produces
   `DataAccessClassification` records;
3. three strategy implementations: `JpaStrategy`, `SpringDataJdbcStrategy`,
   `InMemoryStrategy`;
4. `PersistenceArtifacts` record (entity/record, repository, config);
5. `PersistenceStrategyRegistry` (SPI registry, analogous to
   `TargetEmitterRegistry`);
6. `GET /api/projects/{id}/data-accesses` endpoint returning classified
   accesses with confidence;
7. a Persistence wizard step (table of detected sources, per-source strategy
   selection, model override);
8. profile schema extension `persistence.sourceStrategies`; and
9. CLI command `renovatio persistence <project>` listing classified accesses.

F4 does not deliver:

- real data migration or DDL generation;
- IMS/hierarchical access classification (marked as residual/manual);
- MyBatis, jOOQ, R2DBC, or other persistence strategies;
- generated target-language tests for migrated entities;
- cross-project reusable persistence profiles;
- a replacement parser or second source-language IR; or
- changes to the F2 `TargetEmitter` SPI or semantic IR schema.

## 4. Module and dependency boundary

```text
renovatio-semantic-ir   renovatio-profile
          \                /
        renovatio-shared (emission envelope)
                    |
        renovatio-persistence (classifier + SPI + strategies)
                    |
       provider/core orchestration -> TargetEmitterRegistry
```

`renovatio-persistence` may depend on semantic IR, profile, shared contracts,
and `renovatio-provider-cobol` for the JPA strategy delegation to
`Db2MigrationService`. It must not depend on OpenRewrite, JavaPoet, templates,
React, Spring MVC, or target-language compiler types. Concrete providers may
depend on its result DTOs only through orchestration adapters.

## 5. Data-access classification

### 5.1 Input

The classifier consumes one `SemanticProgram` and its accepted evidence. It
processes two collections:

- `ioOperations` (from F2 §5.3): already classified as `FILE`, `DATABASE`,
  `TERMINAL`, `TRANSACTION`, `MESSAGE`, or `UNKNOWN`.
- `unclassifiedDataAccesses` (from F2 §5.6): residual accesses the COBOL
  projector could not classify.

### 5.2 Classification taxonomy

`DataAccessKind` is a closed enum:

| Kind | Source pattern | Key shape | Record shape |
|---|---|---|---|
| `VSAM_KEY` | `READ ... KEY`, `READ ... INVALID KEY` | Single or composite key from COBOL record | COBOL FD record layout |
| `VSAM_SEQUENTIAL` | `READ NEXT`, `READ PREVIOUS`, `START ... BRACE` | None (position-based) | COBOL FD record layout |
| `SEQUENTIAL_FD` | FD with `INPUT`/`OUTPUT`/`I-O` clause, batch read/write | None | COBOL FD record layout |
| `EXEC_SQL` | `EXEC SQL ... END-EXEC` (DB2 embedded) | SQL table + column references | DB2 table schema |
| `FLAT_FILE_REDEFINES` | FD with `REDEFINES` + discriminator flag | Discriminator value + multiple layouts | Sealed interface + records |
| `RESIDUAL` | F2 `unclassifiedDataAccess` or unknown pattern | Subject text | Unknown |

### 5.3 Classification record

```java
public record DataAccessClassification(
    String id,                    // stable id per §5.5
    String sourceId,              // owning program id
    DataAccessKind kind,
    Optional<String> resourceReference,  // file name, table name, VSAM dataset
    KeyShape keyShape,            // structured key description or NONE
    RecordShape recordShape,      // FD layout, DB2 schema, or UNKNOWN
    Optional<String> discriminatorField, // for REDEFINES: the flag field
    List<DiscriminatorValue> discriminatorValues, // for REDEFINES: mapped layouts
    double confidence,            // 0.0-1.0, classifier certainty
    List<String> evidenceIds,     // F2 evidence references
    SourceProvenance provenance   // from F2
) { }
```

`KeyShape` captures whether the access is key-based (with field list),
position-based, or absent. `RecordShape` captures the FD name or DB2 table
reference and is opaque to the SPI; strategies interpret it.

### 5.4 Classifier implementation

`DataAccessClassifier` is a pure function:

```java
public record DataAccessClassifier() {
    public List<DataAccessClassification> classify(SemanticProgram program) { ... }
}
```

Classification rules:

1. `IoKind.DATABASE` → `EXEC_SQL` (high confidence, operation token from F2).
2. `IoKind.FILE` + F2 `resourceReference` matching a known FD with `REDEFINES`
   → `FLAT_FILE_REDEFINES` when the discriminator field is identifiable from
   the COBOL IR, else `SEQUENTIAL_FD`.
3. `IoKind.FILE` + F2 `resourceReference` matching a known FD without
   `REDEFINES` → `SEQUENTIAL_FD`.
4. `IoKind.FILE` + VSAM key access pattern (from `DataAccessComponent`
   `AccessPattern.READ_BY_KEY`) → `VSAM_KEY`.
5. `IoKind.FILE` + VSAM sequential pattern (`READ_ALL`, `READ_SEQUENTIAL`) →
   `VSAM_SEQUENTIAL`.
6. `UNCLASSIFIED_DATA_ACCESS` → `RESIDUAL`.
7. Confidence is 1.0 when the pattern is unambiguous from F2 data, 0.8 when
   the COBOL IR辅助推断, 0.5 when only source text is available, and 0.0
   for `RESIDUAL`.

### 5.5 Stable identity

Classification ids use lowercase SHA-256 over:

```text
persistence-classification.v1
<normalized programId>
<normalized resourceReference>
 DataAccessKind>
<normalized source path>:<startLine>:<startColumn>:<endLine>:<endColumn>
```

## 6. PersistenceStrategy SPI

### 6.1 Interface

```java
public interface PersistenceStrategy {
    /** Whether this strategy can handle the given classification + target. */
    boolean supports(DataAccessClassification classification,
                     MigrationProfile.Language target);

    /** Emit persistence artifacts for the classified data access. */
    PersistenceArtifacts emit(DataAccessClassification classification,
                              TargetModel targetModel,
                              MigrationProfiles.EffectiveProfile profile);
}
```

`PersistenceArtifacts`:

```java
public record PersistenceArtifacts(
    String entityId,              // generated entity/record class name
    String entitySource,          // full Java source text
    String repositoryId,          // generated repository interface name
    String repositorySource,      // full Java source text
    String configSnippet,         // @Configuration or application.yml fragment
    List<String> diagnostics      // warnings, TODOs, unresolved references
) { }
```

### 6.2 Strategy implementations

#### JPA

- Wraps `Db2MigrationService` for `EXEC_SQL` accesses.
- For `VSAM_KEY`, `VSAM_SEQUENTIAL`, `SEQUENTIAL_FD`, and
  `FLAT_FILE_REDEFINES`: generates `@Entity` with `@Id` from key shape, field
  annotations from record layout, and `JpaRepository` interface.
- `@Transactional` scope derived from `profile.persistence.transactionBoundary`.
- Delegates to existing FreeMarker templates in `renovatio-provider-cobol`.

#### SPRING_DATA_JDBC

- Same entity generation as JPA (shared `EntityGenerator`).
- Repository extends `CrudRepository` instead of `JpaRepository`.
- No `@Transactional` by default (Spring Data JDBC manages transactions
  internally).
- Config snippet declares `spring.data.jdbc.repositories.type=imperative`.

#### IN_MEMORY

- Emits a `Map<Id, Record>` backed stub class.
- Repository is a simple `InMemoryRepository<T>` with `findAll`, `findById`,
  `save`, `deleteById`.
- Used for test environments; ignores transaction boundary.

### 6.3 Registry

`PersistenceStrategyRegistry` mirrors `TargetEmitterRegistry`:

- Constructed from the complete `Set<PersistenceStrategy>` via Spring injection.
- `resolve(kind, target)` returns the sole supporting strategy or throws
  `PersistenceStrategyUnavailableException`.
- Default strategy from `profile.persistence.defaultStrategy` is used when no
  per-source override exists.

## 7. Profile schema extension

Under the open F1 extension namespace:

```json
{
  "renovatio.persistence": {
    "sourceStrategies": {
      "<classification-id>": "JPA"
    }
  }
}
```

The key is the stable classification id from §5.5. Invalid strategy names or
strategies that do not `supports()` the classification are validation errors.
`sourceStrategies` is merged during `MigrationProfiles.effective()` alongside
existing decision overrides.

## 8. REST API

### `GET /api/projects/{id}/data-accesses`

Response:

```json
{
  "projectId": "...",
  "programId": "...",
  "classifications": [
    {
      "id": "...",
      "kind": "EXEC_SQL",
      "resourceReference": "CUSTOMER-MASTER",
      "keyShape": "NONE",
      "recordShape": { "table": "CUSTOMER", "columns": ["ID", "NAME"] },
      "discriminatorField": null,
      "discriminatorValues": [],
      "confidence": 1.0,
      "suggestedStrategy": "JPA",
      "currentStrategy": null
    }
  ],
  "profileHash": "...",
  "diagnostics": []
}
```

- `suggestedStrategy` is the default strategy for the classification's kind and
  target.
- `currentStrategy` reflects any per-source override in the effective profile.
- Returns 404 for unknown project, 422 for analysis not yet run.

## 9. Wizard step

A new "Persistence" step appears after "Decisions" in the wizard:

1. Table of classified data accesses per program: kind, resource reference,
   confidence, suggested strategy.
2. Per-source dropdown to override strategy (JPA, Spring Data JDBC, In Memory).
3. Expandable row showing proposed entity + repository preview (read-only).
4. Override of proposed entity name and key field (stored in profile overlay).
5. "Apply" saves the `sourceStrategies` map to the profile.

Component tests cover: loading state, strategy change, confidence display,
empty state (no data accesses), and validation error when incompatible strategy
is selected.

## 10. Orchestration integration

The emission pipeline becomes:

```
SemanticProgram
  → DataAccessClassifier.classify()
  → PersistenceStrategyRegistry.resolve(kind, target) per classification
  → strategy.emit() per classification
  → Entity/Repository source aggregated into emitted artifacts
  → TargetEmitter.emit() (unchanged)
```

Entity and repository artifacts are added to `EmittedArtifacts` alongside
existing service/controller artifacts. Path collision with existing artifacts
is a deterministic failure.

For `TRANSACTION_SCRIPT`, entities are placed in the same module as the
service. For `HEXAGONAL`, entities go to the entity package and repositories
to the adapter package.

## 11. Failure and compatibility rules

- F4 does not change `TargetEmitter` SPI signatures or behavior.
- Default profile with no `sourceStrategies` produces byte-identical Java output
  to F3. New persistence artifacts are additive only when the profile activates
  per-source strategies.
- `Db2MigrationService` is wrapped, not rewritten; existing direct callers
  continue to work through the JPA strategy.
- JaCoCo thresholds remain unchanged.
- Classification of IMS/hierarchical access produces `RESIDUAL` with a manual
  action item.

## 12. Verification

Completion requires inspectable evidence for:

1. unit tests for classifier covering all six `DataAccessKind` values with
   fixtures for VSAM key, VSAM sequential, sequential FD, EXEC SQL, flat file
   with REDEFINES, and residual;
2. unit tests for each strategy verifying entity/repository source generation
   from representative classifications;
3. registry tests for resolution, default fallback, per-source override, and
   unavailable strategy error;
4. the same COBOL fixture with EXEC SQL producing JPA and Spring Data JDBC
   artifacts that both compile;
5. flat file with REDEFINES producing `sealed interface` + records (or Java
   equivalent) with correct discriminator;
6. JPA strategy for EXEC SQL reproducing the output of the current
   `Db2MigrationService` helpers;
7. REST API contract test for `GET /api/projects/{id}/data-accesses`;
8. React component tests for the Persistence wizard step;
9. profile schema validation including `sourceStrategies` merge;
10. issue-#122 characterization identity for default profile (no
    `sourceStrategies`); and
11. Maven reactor, API, MCP, CLI, and UI regression suites.

## 13. Acceptance mapping

| Agora criterion | Normative sections |
|---|---|
| `data-access-classifier` | §§5.1–5.5 define the taxonomy, records, rules, and identity. |
| `persistence-strategy-spi` | §§6.1–6.3 define the interface, three strategies, and registry. |
| `profile-integration` | §7 defines the schema extension and merge semantics. |
| `api-contract` | §8 defines the REST endpoint and response. |
| `wizard-step` | §9 defines the UI workflow. |
| `orchestration` | §10 defines the pipeline integration. |
| `compatibility` | §§1, 11 define F3 baseline preservation and scope boundaries. |
| `verification-scope` | §12 defines required evidence gates. |
