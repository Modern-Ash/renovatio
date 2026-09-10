# F7 Implementation Plan — renovatio-jcl

Method: Agora `spec-driven` + TDD. Spec: `docs/specs/f7-renovatio-jcl.md`.
Every phase is red→green→refactor; `mvn test` stays green throughout.

## Phase 0: Module skeleton
- New Maven module `renovatio-jcl` in root `pom.xml` (`<modules>`), Java 21,
  `module-info.java` requiring `renovatio-semantic-ir`, `renovatio-profile`,
  `renovatio-decisions`. No dependency back from those modules.
- ArchUnit test: `renovatio-jcl` must not be referenced by parser/IR modules.
- Regression baseline: full `mvn test` green = criterion `defaults-safe` (module inert).

## Phase 1: JCL parser (`parse/`)
- `JclLexer`: column-72 continuation, `//` statements, `/*` delimiter, comments.
- `JclParser` → `JclJob` AST: `JOB`, `EXEC PGM=`, `EXEC PROC=`, `DD` (incl.
  concatenation, `DISP`, `DSN`, `SYSOUT`, `*` in-stream), `PROC`/`PEND`,
  `SET`, `IF/THEN/ELSE/ENDIF`, `INCLUDE` (best-effort).
- Symbolic parameter substitution with precedence EXEC > PROC default > SET > none.
- Catalogued PROC resolution from supplied `.prc/.proc` members; missing → recorded
  `UnresolvedProc` marker (no throw).
- Tests: one per statement form + substitution + in-stream PROC + one-level nested
  PROC + missing PROC (criterion `missing-proc`) + `jcl-parse` happy path.

## Phase 2: `BatchJob` semantic-IR node (`renovatio-semantic-ir`)
- New record `BatchJob { schemaVersion, jobId, sourceProvenance, List<BatchStep>,
  List<BatchDataset>, ConditionGraph }` following `SemanticProgram` conventions
  (immutable, stable ids, deterministic order, dangling-ref rejection).
- `BatchStep`, `BatchDataset` (`AccessKind` enum), `ConditionGraph` with
  `Guard { predicate, referencedStepId, truthTable }`.
- Tests in `renovatio-semantic-ir`: ordering, id stability, validation failures.

## Phase 3: Projection (`ir/BatchJobProjection`)
- `JclJob` + effective `MigrationProfile` → `BatchJob`.
- `CondClause` model + evaluator producing normalized boolean expr + rendered
  truth table (operators `GT GE EQ LT LE NE`, `EVEN`, `ONLY`, multi-predicate OR,
  skipped/abended prior steps, IF-block precedence).
- Tests: `cond-truth-table` (exhaustive operator matrix) + `jcl-parse`
  conditionGraph assertion + dataset access classification (`dd-datasets`).

## Phase 4: Step classification (`classify/`)
- `UtilityCatalog`: `SORT`/`MERGE`, `IEBGENER`, `IDCAMS REPRO/DELETE/DEFINE`.
- `StepClassifier` precedence: migrated program (lookup by `programId` over
  supplied `SemanticProgram` set) → utility → residue (+ action item).
- Tests: precedence, residue routing, unknown utility → action item.

## Phase 5: `batch.target` profile plumbing
- `MigrationProfile` extensions key `batch.target` (enum `SPRING_BATCH`,
  `CLI_PIPELINE`, `SCHEDULER`, `WORKFLOW_ENGINE`); `EffectiveProfileResolver`
  default `SPRING_BATCH` when language JAVA and key absent.
- `BatchDecisionPoints`: `DecisionPoint` category `BATCH` for `batch.target` and
  per-ambiguous-step classification.
- Tests: resolver default + override; decision point emission.

## Phase 6: Spring Batch emitter (`emit/`)
- `BatchEmitter` SPI (mirrors `TargetEmitter`); `SpringBatchBatchEmitter`.
- `@Configuration` per `BatchJob`; `Job` + ordered `Step` beans; flow from
  `ConditionGraph` truth table; `MIGRATED_PROGRAM_CALL` tasklet; utility tasklets;
  `RESIDUE` → tasklet throwing `UnsupportedResidueException` with action-item text.
- `DD` → `FlatFileItemReader/Writer` (SEQ), F4 repository (VSAM), temp resource (TEMP).
- Golden-file tests: `spring-batch-emit`, `dd-datasets`.

## Phase 7: SORT utility template
- Parse `SORT FIELDS=`, `INCLUDE/OMIT COND=`, `SUM FIELDS=`, `INREC/OUTREC`.
- Emit comparator + streaming external sort tasklet.
- Golden fixture `renovatio-jcl/src/test/resources/golden/sort/` → criterion `sort-fixture`.
- Out-of-subgrammar → residue action item, recognised part still emits.

## Phase 8: LLM suggestion (`decision/`)
- Prompt `cobol.jcl.classify-step.v1` (+ fallback YAML "mark residue" + JSON schema).
- `BatchSuggestionAdapter` routes ambiguous steps through
  `DecisionSuggestionService` (category `BATCH`), temp 0, three-hash cache,
  recorded in `renovatio-decisions`. Never auto-applied.
- Tests: schema validation, fallback path, attribution recorded.

## Phase 9: Characterization fixture (#122)
- New `batch-*` fixture: JCL + programs + input datasets + reference outputs.
- Wire into the #122 harness; assert byte-equality of sequential outputs, per-step
  RC, non-persistence of temp datasets → criterion `characterization`.

## Phase 10: Verification / evidence
- `mvn test -pl renovatio-jcl -am` green.
- Full `mvn test` green (regression, `defaults-safe`).
- ArchUnit isolation green.
- test-report artifact = `renovatio-jcl/target/surefire-reports`.

## Criterion → phase map
| Criterion | Covered by |
|---|---|
| jcl-parse | Phase 1, 3 |
| cond-truth-table | Phase 3 |
| spring-batch-emit | Phase 6 |
| sort-fixture | Phase 7 |
| dd-datasets | Phase 3, 6 |
| missing-proc | Phase 1 |
| characterization | Phase 9 |
| defaults-safe | Phase 0, 10 |
