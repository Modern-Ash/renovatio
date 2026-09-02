# F7 renovatio-jcl: Batch Orchestration Migration

- **Work item:** `decision-engine-f7/f7-renovatio-jcl`
- **GitHub issue:** #153 (Epic #152)
- **Method:** Agora `spec-driven` with TDD
- **Status:** governed specification
- **Date:** 2026-09-02
- **Compatibility baseline:** `a02f247e` (F6)
- **Depends on:** F2 (`renovatio-semantic-ir` + `TargetEmitter` SPI), F3 (`renovatio-architecture`)

## 1. Outcome

Renovatio migrates COBOL *programs*. The JCL that sequences them in production
(steps, return-code conditions, datasets, standard utilities) is currently lost,
so the output is a bag of classes with no runnable glue. F7 adds a new
`renovatio-jcl` module that parses JCL, projects it into a target-neutral
`BatchJob` model inside the semantic IR, and emits an orchestration for the
profile-selected `batch.target` (Spring Batch first for Java).

The transformation is deterministic and auditable: same JCL + same profile + same
decisions produce the same orchestration. The LLM only proposes classification
for ambiguous steps/conditions via `DecisionSuggestionService`; it never writes
the final job.

## 2. Binding clarifications

The Spec Owner must accept these before `spec-clarified`. Proposed answers:

| Question | Proposed binding answer |
|---|---|
| Parser boundary | `renovatio-jcl` consumes a project-scoped, deterministically ordered set of JCL member sources (`.jcl`/`.job`/`.prc`) plus the effective migration profile. It produces one `BatchJob` per job card. It does not read PROC libraries outside the supplied workspace; unresolved catalogued PROCs become a manual action item, not a failure. |
| `COND` semantics | Encoded as an explicit truth table: a step is **skipped** when any of its `COND` predicates evaluates true against a prior step's return code. `IF/THEN/ELSE/ENDIF` blocks map to a `conditionGraph` of guarded step groups. Every emitted guard carries the source predicate and its evaluated truth table as evidence. Human review of the `conditionGraph` is a required criterion. |
| Step classification | Per step, in precedence order: (1) `EXEC PGM=` matching a migrated program → call into the migrated artifact; (2) recognised standard utility (`SORT`/`MERGE`, `IEBGENER`, `IDCAMS REPRO/DELETE/DEFINE`, `ICETOOL` subset) → deterministic template; (3) otherwise → residue with a manual action item. LLM suggestion (category `BATCH`) only annotates (3) and ambiguous (2); never auto-applied. |
| DD / dataset mapping | Sequential `DD` → file/stream resource; VSAM `DD` → repository via F4 `PersistenceStrategy`; `DISP=(NEW,PASS)` temp datasets → in-memory/intermediate step artifact that does not persist; `SYSOUT`/`SYSIN` → job stdio. GDG, catalog, RACF are out of scope and marked residue. |
| Target selection | New profile key `batch.target` in `MigrationProfile` extensions: `SPRING_BATCH` \| `CLI_PIPELINE` \| `SCHEDULER` \| `WORKFLOW_ENGINE`. F7 activates `SPRING_BATCH` only; the others are storable but inactive. Default when absent and language is JAVA: `SPRING_BATCH`. |
| Determinism of `SORT` | F7 supports the common subgrammar: `SORT FIELDS=(pos,len,fmt,order...)`, `INCLUDE/OMIT COND=`, `SUM FIELDS=`, `INREC/OUTREC` field lists. Anything outside the subgrammar → residue with a manual action item; the recognised part still emits. |

Artifact registration, criterion stages, and lifecycle transitions remain
separate durable Agora actions.

## 3. Scope and non-goals

F7 delivers:

1. **`renovatio-jcl` module** — `JclParser` producing a `JclJob` AST
   (`JOB`, `EXEC PGM=`, `EXEC PROC=`, `DD`, `COND`, `IF/THEN/ELSE`, `SET`,
   in-stream and catalogued PROCs, symbolic parameter substitution).
2. **`BatchJob` semantic-IR node** — `BatchJob { steps[], datasets[], conditionGraph }`,
   immutable, deterministically ordered, validated the same way as
   `SemanticProgram` (stable ids, no dangling refs).
3. **`StepClassifier`** — the precedence rule above; emits `BatchStep` kinds
   `MIGRATED_PROGRAM_CALL`, `STANDARD_UTILITY`, `RESIDUE`.
4. **`batch.target` profile plumbing** — key in `MigrationProfile` extensions,
   `EffectiveProfileResolver` support, `DecisionPoint` of category `BATCH`.
5. **`SpringBatchBatchEmitter`** — implements the batch emission SPI; produces a
   `@Configuration` `Job` with ordered `Step` beans, flow guarded by the
   `conditionGraph`, tasklets for utilities, and item readers/writers for `DD`
   resources. Emitters stay unaware of JCL; they consume `BatchJob`.
6. **Utility templates** — `SORT` (comparator + streaming external sort),
   `IEBGENER`/`IDCAMS REPRO` (copy), `IDCAMS DELETE/DEFINE` (resource lifecycle).
7. **LLM suggestion** — new `BATCH` category prompt for ambiguous step/condition
   classification, validated against a JSON schema, cached, attributed. Fallback
   is "mark as residue".
8. **Characterization harness hook (#122)** — a `BatchJob` fixture runs the
   emitted orchestration and asserts the produced data outputs equal the
   reference batch outputs.

F7 does **not** deliver:

- More than one active `batch.target` (only `SPRING_BATCH`).
- The full `SORT`/`IDCAMS`/`ICETOOL` grammar (common subset only).
- GDG, system catalog, RACF, mainframe schedulers (OPC / Control-M), JCL→JCL.
- LLM generating final job code (classification suggestion only).
- Migration of the programs themselves (owned by F1–F6).

## 4. Module layout

```
renovatio-jcl/
  src/main/java/org/shark/renovatio/jcl/
    parse/     JclParser, JclJob, JclStep, DdStatement, CondClause, JclLexer
    ir/        BatchJobProjection  (JclJob + profile -> BatchJob)
    classify/  StepClassifier, UtilityCatalog
    emit/      BatchEmitter (SPI), SpringBatchBatchEmitter, util templates
    decision/  BatchDecisionPoints, BatchSuggestionAdapter
  src/main/resources/prompts/  cobol.jcl.classify-step.v1.yaml (+ fallback + schema)
  src/test/...  parser tests, COND truth-table tests, projection tests,
                emitter golden tests, characterization fixture
```

`BatchJob` lives in `renovatio-semantic-ir` (new node type) so the IR stays the
single neutral model; everything else is in `renovatio-jcl`.

## 5. `BatchJob` model

| Field | Notes |
|---|---|
| `jobId` | normalized from the `JOB` card name |
| `steps` | ordered `List<BatchStep>`; id = hash(jobId, stepName, ordinal) |
| `datasets` | ordered `List<BatchDataset>`; ddName, access (`SEQ_IN`, `SEQ_OUT`, `VSAM`, `TEMP`, `STDIO`), resourceReference |
| `conditionGraph` | ordered guarded groups: `Guard { predicate, referencedStep, truthTable }` → member step ids |
| `sourceProvenance` | source path + span, matching `SemanticProgram` convention |

`BatchStep`: `id`, `stepName`, `kind`, `programRef` (nullable), `utility`
(nullable), `datasetRefs`, `residueReason` (nullable).

## 6. `COND` truth table

For `COND=(code,op)` on a step: skip the step when `returnCode op code` is true
(`op` ∈ `GT GE EQ LT LE NE`). `COND=EVEN` / `COND=ONLY` handled explicitly.
`COND=(0,NE,STEP1)` references a specific prior step. The projection produces,
for each step, a normalized boolean expression over prior return codes plus a
rendered truth table (all relevant RC values → run/skip) that is attached as
evidence and shown in the UI diff. Tests cover every operator and the
`EVEN`/`ONLY` cases.

## 7. Spring Batch emission

- One `@Configuration` class per `BatchJob`; `Job` bean named from `jobId`.
- Each `BatchStep` → a `Step` bean. `MIGRATED_PROGRAM_CALL` → tasklet invoking
  the migrated program's entry point with mapped dataset resources.
  `STANDARD_UTILITY` → utility template tasklet/chunk. `RESIDUE` → a tasklet that
  throws `UnsupportedResidueException` with the action-item text (never a silent
  no-op).
- `conditionGraph` → Spring Batch flow with `on(...).to(...)` transitions derived
  from the truth table; the exit status of each step exposes its return code.
- `DD` resources → `FlatFileItemReader`/`Writer` (SEQ), repository (VSAM, via F4),
  `ByteArrayResource`/temp path (TEMP).
- Golden-file tests compare generated sources byte-for-byte against
  `src/test/resources/golden/`.

## 8. Determinism, invariants

- Deterministic ordering everywhere (steps by ordinal, datasets by ddName,
  guards by step ordinal).
- **Defaults = current behaviour**: a project with no JCL members produces
  exactly today's output; the module is inert.
- LLM output: temperature 0, JSON-schema validated, three-hash cache key,
  prompt/model/version/rationale recorded in `renovatio-decisions`.
- Characterization harness (#122) is a merge gate.
- `COND` inversion risk mitigated by the explicit truth table + dedicated tests +
  mandatory human review criterion.

## 9. Acceptance criteria

| id | Criterion |
|---|---|
| `jcl-parse` | A 3-step JCL with `COND` parses to a `BatchJob` whose `conditionGraph` matches the expected guarded groups. |
| `cond-truth-table` | `COND` skip-on-true semantics covered by an explicit truth table and dedicated tests for every operator + `EVEN`/`ONLY`. |
| `spring-batch-emit` | A 3-step chained job emits a Spring Batch config that runs the 3 migrated programs in order and honours the condition. |
| `sort-fixture` | `SORT FIELDS=` + `INCLUDE COND=` produces output identical to a reference fixture. |
| `dd-datasets` | Sequential in/out `DD` → correct files; `DISP=(NEW,PASS)` temp dataset does not persist. |
| `missing-proc` | A missing catalogued PROC yields a manual action item, no crash. |
| `characterization` | The emitted orchestration produces the same data outputs as the original batch for the fixture (#122 harness). |
| `defaults-safe` | A project with no JCL members produces byte-identical output to the F6 baseline. |

## 10. Test plan (TDD)

1. `JclLexer` / `JclParser` unit tests: job card, `EXEC PGM`/`PROC`, `DD` forms,
   symbolic substitution, in-stream PROC, `IF/THEN/ELSE`, `SET`.
2. `CondClause` truth-table tests (criterion `cond-truth-table`).
3. `BatchJobProjection` tests: ordering, id stability, dangling-ref rejection,
   dataset access classification.
4. `StepClassifier` tests: precedence, residue routing, missing PROC.
5. `SpringBatchBatchEmitter` golden tests (criteria `spring-batch-emit`,
   `dd-datasets`).
6. `SORT` utility template test against reference fixture (`sort-fixture`).
7. Characterization fixture wired into the #122 harness (`characterization`).
8. Regression: full `mvn test` with no JCL fixture proves `defaults-safe`.
