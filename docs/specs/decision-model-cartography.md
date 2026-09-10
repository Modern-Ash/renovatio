# Decision-Model Cartography — COBOL→Java Generation Path

- **Work item:** `decision-engine/f0-decision-cartography`
- **GitHub issue:** #145 (Epic #152)
- **Type:** Spike — no production code
- **Status:** governed; durable lifecycle state is recorded in Agora
- **Date:** 2026-09-01

## 1. Purpose and resolved boundaries

This document inventories implicit decisions in the current COBOL→Java path,
maps every direct and transitive input consumed by `JavaGenerationService` and
`MigrationPlanService`, assesses reuse of the semantic IR and `renovatio-llm`,
and recommends an evidence-based F1 scope.

The Spec Owner resolved the clarification questions as follows:

1. This document is the single required logical `spec` artifact. Multiple
   registry rows with the identical
   `repo://docs/specs/decision-model-cartography.md` URI are successive
   content-addressed registrations of this same document, not separate
   artifacts requiring reconciliation.
2. Fixture evidence is pinned to
   `renovatio-provider-cobol/src/test/resources/characterization/` at the PR
   base revision `b430ba48a01ebe55e42b9714a5ccf5557e3981aa`.
3. The coupling map includes parameters, collaborators, configuration,
   filesystem and ambient state, static state, and transitive dependencies.
4. IR reuse is judged from its API, available goldens, and generation call
   sites. LLM reuse is judged against the annotation/action-item seam and the
   rule that deterministic high-confidence decisions stay deterministic while
   only residual ambiguity may become a gated suggestion.
5. F1 uses a strict selection rule: frequency at least 3, or structural
   applicability to every program; deterministic confidence H; and
   characterization-verifiable. No exception is made for a desirable cluster.
6. The Spec Owner accepts the content of this registered specification as the
   basis for the four acceptance criteria. Criterion-stage marking and
   lifecycle transitions remain separate durable Agora actions; this document
   intentionally does not duplicate their current values.
7. The authoritative reproducibility snapshot is the clean PR base revision
   `b430ba48a01ebe55e42b9714a5ccf5557e3981aa`. The two service files are
   additionally identified by content SHA-256 in §2.2.
8. The Spec Owner accepts per-object WORKING-STORAGE lifetime as the real
   PERSISTENCE-category fixture example, while retaining the explicit corpus
   gap for file, VSAM, SQL, and CICS persistence.
9. Agora's durable lifecycle state is authoritative. The status line in this
   document is informational and does not itself transition governed work.

## 2. Evidence base and counting method

### 2.1 Canonical fixture corpus

| Fixture | Principal construct |
|---|---|
| `compute-decimal-sign` | `COMPUTE`; signed scaled decimals |
| `move-alphanumeric-boundaries` | alphanumeric padding/truncation |
| `move-numeric` | signed scaled numeric truncation |
| `evaluate-level-88` | `EVALUATE TRUE`; level-88 conditions |
| `if-nested` | nested `IF/ELSE/END-IF` |
| `goto-reducible` | reducible `GO TO` |
| `goto-irreducible` | `GO TO ... DEPENDING ON`; back-edge |
| `perform-simple-nested` | nested paragraph `PERFORM` |
| `redefines-overlap` | overlapping `REDEFINES` views |
| `data-intent-redefines` | `REDEFINES` plus accepted annotation |
| `odo-valid-boundary` | valid ODO upper boundary |
| `odo-invalid-count` | ODO count outside the declared bound |
| `unsupported-construct` | `ALTER ... TO PROCEED TO` |

All 13 fixtures contain `input.cob`, `expected-behavior.json`, and
`expected-action-items.json`. Ten contain `expected-ir.json`; it is absent from
`goto-irreducible`, `odo-invalid-count`, and `unsupported-construct`. Only
`data-intent-redefines` and `move-numeric` carry both an annotated sidecar and
generated-Java goldens.

“Frequency” is the number of distinct fixture inputs in which the relevant
source construct occurs. A policy applied unconditionally by the generator is
counted against all 13 inputs and labeled **structural**. A decision driven by
project configuration rather than fixture content is labeled **n/a**. These
three cases are not mixed.

### 2.2 Code snapshot

The coupling map was regenerated from a clean checkout of the pinned PR base:

| Source | Inspected content SHA-256 |
|---|---|
| `JavaGenerationService.java` | `8ec5359ece8a48cc0c8891f235c770a9a5ac7dddc6c79e024f581a32361890c3` |
| `MigrationPlanService.java` | `2e44a17db423b8a70d576aeaa89475f1cfe3e24d057e04fb1ece991dcd4803be` |

These are the versions stored in both the PR parent and resulting tree. They
do not produce or consume `StubResult.metadata.outputPath/generatedFiles`.
No production source was changed by this spike.

## 3. Complete coupling map

### 3.1 `JavaGenerationService.generateInterfaceStubs`

| Consumed input | Origin | Consumption and effect |
|---|---|---|
| `query.parameters["dialect"]` | `NqlQuery` | Forwarded to `CobolParsingService`; selects IBM, GNU, or Micro Focus parsing, with IBM fallback. |
| `workspace.path` | `Workspace` | Root for recursive source/copybook discovery, generated output, and the manual-action report. |
| `workspace.metadata["dialect"]` | `Workspace` | Dialect fallback when the query does not specify one. |
| `workspace.metadata["outputDir"]` | `Workspace` | Absolute or workspace-relative generated-source directory; default is `generated-java-stubs`. |
| Source-tree paths and contents | Filesystem | `.cob`, `.cobol`, and `.cbl` files are discovered recursively and read; `.cpy` files are discovered for analysis summary. File names seed generated class names. |
| `AnalyzeResult.success/message` | `CobolParsingService` | Controls failure return and error message. |
| `AnalyzeResult.data["programs"]` | `CobolParsingService` | Defines the programs iterated by the generator. Other analysis keys (`sourceFiles`, `copybooks`, `summary`, performance) are produced but not consumed here. |
| `CobolProgram.metadata["filePath"]` | Parser result | Supplies base class name, IR path, diagnostic source, and sibling sidecar location. |
| `metadata["entries"][*].name` | Parser result | Chooses per-entry interface/implementation methods; an empty list produces only `process`. The parser also produces `using`, but generation does not consume it. |
| `metadata["linkageItems"][*].name/javaType` | Parser result | DTO fields when entries exist. `linkageStructName` is produced but not consumed. |
| `metadata["dataItems"][*].name/javaType` | Parser result | DTO fields when entries do not exist. |
| `metadata["cicsCommands"]` | Parser result | Non-empty set enables a CICS controller; values become template transactions. |
| `metadata["source"]` | Optional caller metadata | IR text fallback only when `filePath` is a non-null path that does not exist. A missing/null `filePath` fails earlier during class-name/path construction, and the current parser does not populate `source`. |
| Parser regexes and defaults | `CobolParsingService` static state | Determine program id, entries, CICS commands, recognized data items, PIC→Java mapping, file extensions, and default IBM dialect. |
| `CobolIntermediateModelService` | Constructor collaborator | Selects path-versus-string IR parsing, wraps path-read failures, and delegates all IR construction to its injected/default `SimpleCobolIrParser`. |
| `SimpleCobolIrParser` grammar and rules | Transitive IR collaborator | Regexes and normalization determine program id, data declarations/PIC/redefines/level-88s, paragraph and ENTRY boundaries, statement kinds, CFG, Java-name collisions, diagnostics, execution context, control-break detection, and decomposition. |
| `CobolIntermediateModel.programId` | Semantic IR | Labels diagnostics/action items and annotated processing. |
| `CobolIntermediateModel.dataItems[*].name/picture/javaType` | Semantic IR | Refines validation precision, scale, sign, length, and Java type for DTO fields recognized by the lightweight parser. |
| Remaining IR graph | Semantic IR | Paragraphs, statements, CFG, diagnostics, execution context, and decomposition are passed through `ExecutionContext` to `PopulateCobolProcessRecipe`; the identity projector also consumes the model when validating annotations. |
| Sibling `*.annotated.json` bytes | Filesystem | Schema, base-IR hash, node identities, annotation payload, provenance, confidence, and review state decide whether enrichment is accepted or becomes a diagnostic. |
| Identity projection rules | `CobolIrIdentityProjector` | Canonical closed-set projection of the complete base model, sorted paths/maps, supported node kinds, canonical node ids, and base-IR hashing determine which sidecars still match. Unknown identity-bearing types fail closed. |
| Annotation validation rules | JSON Schema + `AnnotatedCobolValidator` | Enforce schema/base versions, base hash, canonical annotation/output hashes, node existence/kind, duplicate identities, conflicting outputs, and referenced control-flow nodes. |
| `ObjectMapper` modules and guardrail schema | Constructor/static catalog | Decode and validate sidecars and serialize the action-item report. |
| `TemplateCodeGenerationService` | Constructor collaborator | Consumes generated CICS class name and transaction set only when CICS commands exist. |
| `CobolSemanticTranspiler` / OpenRewrite runtime | Constructor collaborator | Consumes generated Java, IR, optional annotated context, Java parser/runtime classpath, recipe results, validation errors, and dropped-annotation outcomes. |
| Action-item redaction/serialization policy | `ManualActionItemWriter` + `SensitiveValueRedactor` | Sorts items/map entries, emits schema `manual-action-item.v1`, and redacts authorization, bearer, API-key, token, secret, password, and `sk-...` patterns before persistence. |
| Writable filesystem and replacement semantics | Ambient/static | Creates output directories/files and `build/reports/renovatio/manual-action-items.json`; the report is written to a same-directory temporary file, atomically moved with replacement, and the temporary file is deleted in `finally`. Generated Java files use direct replacement writes. Failures affect returned output text or overall generation. |
| Hard-coded Java choices | Static code | Package `org.shark.renovatio.generated.cobol`, Spring `@Service`, interface+implementation+DTO layout, JavaPoet formatting, and fallback naming are unconditional inputs to output. |
| Locale, filesystem existence, standard streams | Ambient | `Locale.ROOT` normalizes names/PIC clauses; file existence selects path-vs-source IR parsing; `System.out/err` receives diagnostics. |

Project `javaPackage`, `javaArchitecture`, target language, persistence strategy,
and numeric rounding policy are not consumed by this service. The first two are
stored by the API layer but do not reach generation.

### 3.2 `MigrationPlanService`

| Consumed input | Origin | Consumption and effect |
|---|---|---|
| `query` | `createMigrationPlan` caller | Analyzed, stored in the plan, and later reused for baseline analysis and Java generation. Its dialect parameter is consumed transitively. |
| `scope` | caller | Stored on the plan but never read when creating steps or applying the plan. |
| `workspace` | caller | Forwarded to analysis, generation, and diff; its path/metadata/filesystem effects are those listed above. The stored `plan.workspace` is never read. |
| Initial `AnalyzeResult.success/message` | parser | Failure prevents plan creation. `data`, performance, and the `analyzeResult`/`query` parameters passed to `createMigrationSteps` do not affect the five fixed steps. |
| `planId` | apply caller | Key into the process-local `activePlans` map. |
| `dryRun` | apply caller | Stored on `MigrationRun` and echoed in result changes; it does not suppress generation or writes. |
| Stored plan `query/steps` | `activePlans` | Query drives baseline/generation; step type selects the only implemented branch and description is recorded. |
| `StubResult.success/generatedCode` | `JavaGenerationService` | Successful, non-null generated code is accumulated into run output. The result message and metadata are ignored. |
| Baseline `AnalyzeResult` and synthetic migrated metrics | parser/local | Passed to `BenchmarkUtils.compare`; elapsed time becomes the migrated performance metric. |
| `runId` | diff caller | Key into process-local `completedRuns`. |
| Stored run fields | `completedRuns` | `planId`, timestamps, and generated-file keys form the textual/semantic diff. The diff method's `workspace` parameter is unused. |
| Fixed step definitions | Static code | Always creates `PARSE_COBOL`, `GENERATE_JAVA_DTOS`, `GENERATE_JAVA_STUBS`, `CREATE_MAPPINGS`, and `GENERATE_TESTS`; only `GENERATE_JAVA_STUBS` executes behavior. |
| `activePlans` / `completedRuns` | Instance state | Unsynchronized, non-persistent `HashMap` state determines lookup and diff availability. |
| UUID, wall clock, monotonic clock | Ambient/static | Generate ids, timestamps, and elapsed-time metrics. |

## 4. Decision-point catalog

Confidence is the expected reliability of a deterministic heuristic: H, M, or
L. “LLM?” means only that residual ambiguity may be proposed through the
governed annotation/action-item seam. “Char?” means the choice can be checked
with an observable behavior or golden-output characterization.

| # | Category | Decision point | Typical/current location | Current option | Alternatives | Conf. | LLM? | Char? | Freq. |
|---:|---|---|---|---|---|:---:|:---:|:---:|---:|
| 1 | NUMERIC | Unscaled numeric PIC→Java type | parser mapper + IR mapper | Parser: ≤9 `Integer`, >9 `Long`; IR: ≤9 `Integer`, ≤18 `Long`, then `BigDecimal` | always decimal; primitive/wrapper; big integer | H | no | yes | 6 |
| 2 | NUMERIC | Scaled decimal representation | both mappers | `BigDecimal` | scaled `long`; decimal wrapper | H | no | yes | 2 |
| 3 | NUMERIC | `COMP-3` representation | both mappers | parser uses `BigDecimal`; IR retains usage but maps by digits/scale | packed codec; annotated decimal | M | no | yes | 0 |
| 4 | NUMERIC | `COMP-5` width/sign | both mappers | width selects integer type; storage/sign do not affect Java type | explicit binary wrapper; promotion | M | no | yes | 0 |
| 5 | NUMERIC | Signed `S9` handling | `PicType`; generated validation | same Java type; sign only permits negative validation | signed wrapper; policy guard | H | no | yes | 2 |
| 6 | NUMERIC | `COMPUTE` rounding/intermediate precision | IR exists; generator behavior absent | no emitted arithmetic | COBOL rounding; configured rounding | L | yes | yes | 1 |
| 7 | NUMERIC | Numeric `MOVE` truncation | IR exists; generator behavior absent | no emitted assignment semantics | truncating runtime; guarded proposal | M | yes | yes | 1 |
| 8 | DATA_SHAPE | Alphanumeric `MOVE` padding/truncation | IR exists; generator behavior absent | no emitted assignment semantics | PIC-aware setter/runtime | M | yes | yes | 1 |
| 9 | DATA_SHAPE | `REDEFINES` Java shape | IR `redefines`; DTO path ignores aliasing | unrelated fields/no shared storage | overlay; union; accessors | L | yes | partial | 2 |
| 10 | DATA_SHAPE | Fixed `OCCURS` collection shape | IR `occurs`; DTO path ignores it | no collection emitted | list; fixed array | M | no | yes | 0 |
| 11 | DATA_SHAPE | ODO count linkage | source fixtures; IR lacks depending-on field | no bounded dynamic shape | bounded list+count; array+guard | L | yes | yes | 2 |
| 12 | DATA_SHAPE | Group nesting vs flattening | lightweight linkage parser | level-05 linkage fields flattened | nested class; record | M | no | yes | 2 |
| 13 | DATA_SHAPE | Level-88 representation | first-class IR model; generator ignores it | no generated predicate | boolean predicate; enum | M | yes | yes | 1 |
| 14 | DATA_SHAPE | Edited/display PIC representation | parser fallback | `String` | numeric backing+formatter | L | yes | yes | 0 |
| 15 | CONTROL_FLOW | Paragraph→method granularity | IR paragraphs; generator/recipe | one generic service entry path | method per paragraph; inlining | M | yes | yes | 13 structural |
| 16 | CONTROL_FLOW | `PERFORM` call/loop form | first-class IR; generator gap | no direct generated form | call; `for`; `while` | M | yes | yes | 1 |
| 17 | CONTROL_FLOW | Reducible `GO TO` restructuring | CFG/diagnostics; no `GotoStatement` | no generated form | structured branches/loop | L | yes | yes | 1 |
| 18 | CONTROL_FLOW | Irreducible `GO TO`/`ALTER` | diagnostics/action items | manual action item | dispatcher/state machine | L | yes | no | 2 |
| 19 | CONTROL_FLOW | `IF`/`EVALUATE` Java form | first-class IR; generator gap | no direct generated form | `if`; `switch` | H | no | yes | 2 |
| 20 | CONTROL_FLOW | Program termination form | generator gap | no emitted termination | return; domain exception | M | no | yes | 12 |
| 21 | CONTROL_FLOW | Error clauses | generator gap | no emitted handling | branch; exception; action item | L | yes | yes | 0 |
| 22 | PERSISTENCE | Sequential/file I/O target | first-class operation IR; generator gap | no generated persistence | NIO; repository port | L | yes | partial | 0 |
| 23 | PERSISTENCE | VSAM/indexed access target | generator gap | no generated persistence | key-value; JPA; repository | L | yes | no | 0 |
| 24 | PERSISTENCE | Embedded SQL target | first-class `Db2Statement`; generator gap | no generated persistence | JDBC; Spring Data; MyBatis | M | yes | yes | 0 |
| 25 | PERSISTENCE | CICS transaction adapter | CICS template gate | controller generated when commands exist | messaging; command handler; omit | M | no | yes | 0 |
| 26 | PERSISTENCE | Working-storage lifetime | generated DTO instances | per invocation/object | singleton; conversation; external store | M | no | yes | 11 |
| 27 | NAMING | COBOL→Java identifier | three local helpers | divergent camel/pascal/sanitization rules | shared mapper; dictionary; guard | H | no | yes | 13 structural |
| 28 | NAMING | Generated package | JavaPoet/ClassName literals | fixed package | project/profile package | H | no | yes | 13 structural |
| 29 | NAMING | Class layout per program | generator | DTO+interface+implementation | aggregate; single class | M | no | yes | 13 structural |
| 30 | NAMING | Getter/setter convention | generator helpers | JavaBean `get`/`set` | boolean `is`; fluent; records | H | no | yes | 11 |
| 31 | ARCHITECTURE | Target architecture | API stores value; generator ignores it | flat service layout | MVC; hexagonal; clean | H | no | yes | n/a |
| 32 | ARCHITECTURE | Target language | service selection | Java-only in this path | Python; Node; pluggable target | H | no | yes | n/a |
| 33 | ARCHITECTURE | Framework coupling | implementation generator | Spring `@Service` always | framework-free; CDI; profile toggle | H | no | yes | 13 structural |
| 34 | ARCHITECTURE | Interface/implementation split | generator | split always | single class; functions | M | no | yes | 13 structural |
| 35 | ARCHITECTURE | Migration-step composition | plan service | five fixed steps; one executable | capability/profile-driven steps | H | no | yes | n/a |
| 36 | ARCHITECTURE | Sidecar vs inferred intent | annotated resolver | validated sibling sidecar or deterministic fallback | interactive/LLM proposal | M | yes | partial | 2 |
| 37 | NUMERIC | Default usage when omitted | `PicClause`/parser | DISPLAY | dialect/profile default | H | no | yes | 11 |
| 38 | DATA_SHAPE | `VALUE` initializer policy | lightweight parser/generator gap | initial values dropped | field/constructor initialization | H | no | yes | 9 |

### 4.1 Real fixture example for every category

| Category | Fixture-cited example |
|---|---|
| NUMERIC | `move-numeric`: `S9(5)V99` and `S9(3)V99` expose scaled/sign/truncation decisions (#2, #5, #7). |
| CONTROL_FLOW | `if-nested` and `evaluate-level-88` expose the `if`/`switch` decision (#19). |
| DATA_SHAPE | `redefines-overlap` exposes shared-storage versus independent-field shape (#9). |
| PERSISTENCE | `compute-decimal-sign` declares WORKING-STORAGE values whose lifetime becomes per-object generated DTO state (#26); the corpus has no file, VSAM, SQL, or CICS example. |
| NAMING | Every fixture has `PROGRAM-ID`; for example `MOVE-ALPHA` becomes a Java class/method naming input (#27), while package policy is structural (#28). |
| ARCHITECTURE | Every generated service implementation, including `compute-decimal-sign`, receives Spring `@Service` and an interface/implementation split (#33, #34). |

## 5. Semantic-IR reuse assessment

| Area | Evidence and finding |
|---|---|
| Types/data | `CobolDataItem` already carries name, picture, level, occurs, redefines, Java type, rich `PicType`, and `Level88Condition` values. `PicClause` preserves digits, scale, signedness, and usage. This is the authoritative substrate for numeric/data decisions; a parallel model would duplicate semantics. |
| Control flow | `CobolIntermediateModel` carries paragraphs and a CFG. First-class statements include `MoveStatement`, `ComputeStatement`, `IfStatement`, `EvaluateStatement`, `PerformStatement`, `CallStatement`, `FileOperationStatement`, and `Db2Statement`. The generator currently consumes little of this surface, but the substrate exists. |
| Known IR gaps | There is no first-class `GotoStatement` or `ALTER` model, `CobolDataItem.occurs` does not retain the ODO depending-on identifier, and file-operation statements do not describe full SELECT/FD/index metadata. These are focused enrichment gaps, not evidence that the whole procedure IR is absent. |
| Current seam | Java generation builds DTO shape from lightweight parser metadata, uses IR data items only to refine validation, and passes the full IR to OpenRewrite. F1 can expose selected decisions over the existing IR without replacing it. |
| Verdict | Reuse the semantic IR as the decision substrate. F1 should reconcile duplicate parser/IR mappings only for decisions admitted by the strict cut; later phases can extend the identified gaps. |

## 6. `renovatio-llm` reuse assessment

The existing `AnnotatedContextResolver` validates schema, base-IR hash, node
identity, provenance, confidence, and review state. `CobolSemanticTranspiler`
passes accepted annotations to deterministic OpenRewrite recipes, while
`AnnotationActionItemFactory` and `ManualActionItemWriter` retain rejected or
unresolved outcomes for human review.

That is the reusable governance seam for future LLM suggestions. An LLM may
propose only options marked “LLM? yes”; it must not mutate semantic output
without schema validation, deterministic checks, characterization evidence,
and review. F1's strict selection contains only high-confidence deterministic
decisions, so F1 needs no LLM-backed decision producer and no new transport.

## 7. Strict prioritization result

Applying the confirmed rule mechanically yields exactly seven F1 candidates:

| Decision | Frequency basis | Why admitted |
|---|---|---|
| #1 unscaled numeric type | 6 | ≥3, confidence H, characterizable |
| #27 identifier mapping | 13 structural | universal, confidence H, characterizable |
| #28 generated package | 13 structural | universal, confidence H, characterizable |
| #30 accessor convention | 11 | ≥3, confidence H, characterizable |
| #33 framework coupling | 13 structural | universal, confidence H, characterizable |
| #37 default usage | 11 | ≥3, confidence H, characterizable |
| #38 `VALUE` initializer | 9 | ≥3, confidence H, characterizable |

Notably excluded by the strict rule are scaled decimals (#2), signed PIC (#5),
and IF/EVALUATE form (#19), each present in only two fixtures. High-frequency
decisions with confidence M (#15, #20, #26, #29, #34) are also excluded. The
result is intentionally evidence-driven rather than a preselected semantic
cluster.

## 8. Recommended F1 scope cut

F1 (#146) should make the seven admitted decisions explicit and
profile-resolvable, with a deterministic default, provenance of the resolved
value, confidence, and a reference to its characterization guard. It should:

1. establish one versioned decision/profile contract for #1, #27, #28, #30,
   #33, #37, and #38;
2. remove divergent resolution of the admitted type/default-usage and naming
   decisions, using the existing semantic IR as the source of COBOL facts;
3. make package, accessor, framework coupling, and initial-value choices
   observable in generated golden output;
4. retain deterministic fallback when a profile omits a value; and
5. add characterization coverage for every admitted option and default.

F1 should not include scaled-decimal/sign policy, statement-level translation,
`REDEFINES`/ODO Java shape, architecture-style transformation, target-language
selection, persistence generation, or LLM-backed suggestions. Zero-frequency
gaps (`COMP-3`, `COMP-5`, edited PIC, SQL, VSAM, fixed `OCCURS`, error clauses,
and CICS) should become explicit corpus-expansion work; disabled tests are not
evidence for this F1 cut.

The rationale is narrow: the current path already emits Java structure but
resolves these seven ubiquitous/high-frequency choices through hard-coded or
duplicated rules. Parameterizing only that deterministic surface creates the
smallest characterization-verifiable foundation for later semantic,
architecture, persistence, target, and LLM phases.
