# F2 Semantic IR and `TargetEmitter` SPI

- **Work item:** `decision-engine-f2/f2-semantic-ir-emitter-spi`
- **GitHub issue:** #147 (Epic #152)
- **Method:** Agora `spec-driven` with TDD
- **Status:** governed draft; durable Agora state is authoritative
- **Date:** 2026-09-01
- **F1 compatibility baseline:** `152d57462d4d7fc2b4554b6a1f029ae44e96d97a`

## 1. Outcome

F2 inserts a target-neutral semantic model between COBOL analysis and artifact
emission, then makes target selection an explicit SPI operation. The existing
Java path becomes the first adapter behind that SPI. The effective F1 profile
and decision identity travel with the semantic program all the way to the
emitter.

For the default F1 profile, F2 is a structural refactor: every production Java
artifact key and every UTF-8 byte covered by the issue-#122 characterization
corpus remains identical. F2 does not implement Node or Python emission,
architecture transformation, or a detailed persistence model.

## 2. Resolved clarifications

| Question | Binding answer |
|---|---|
| Semantic model contract | Version `1`, immutable and null-safe records, defensive immutable collections, deterministic ordering, stable node identities derived from program id, node kind, source span, and semantic role, and preserved source provenance. No Java AST, compiler, import, annotation, OpenRewrite-Java, or template type may enter the module. |
| `TargetModel` boundary | A one-to-one immutable envelope over one semantic program, the effective F1 `MigrationProfile`, resolved decisions, applied decision ids, profile hash, and source provenance. F2 performs no semantic-IR-to-IR graph reshaping. |
| Data-intent compatibility | `@CobolDataIntent` keeps its exact type, member values, placement, ordering, and emitted bytes. It is projected deterministically from neutral data intents; it is never read back as semantic truth. Existing downstream consumers remain compatible. |
| Registry coverage | Every active production generation request resolves exactly one emitter through `TargetEmitterRegistry`. A missing emitter raises `TARGET_EMITTER_UNAVAILABLE` with the requested target and sorted available targets. Duplicate registrations fail deterministically during registry construction. |
| Baseline and evidence | Use F1 merge commit `152d574` after rebasing. Require issue-#122 byte identity, ArchUnit isolation, focused module tests, functional reactor, MCP and CLI regression checks, plus an attempted literal `mvn clean install`. JaCoCo thresholds may not be weakened; an unchanged baseline-only failure is recorded explicitly rather than hidden. |

The Spec Owner accepted these answers on 2026-09-01. Artifact registration,
criterion stages, and lifecycle transitions remain separate durable Agora
actions.

## 3. Baseline and current seams

The source baseline is F1 merge commit
`152d57462d4d7fc2b4554b6a1f029ae44e96d97a`. F2 was rebased onto that commit
before opening its pull request and recording final review evidence.

At that baseline:

- `JavaGenerationService` and `MigrationPlanService` live in
  `renovatio-provider-cobol`;
- `CobolLanguageProvider.generateStubs` invokes the Java service directly;
- `renovatio-provider-java` contains Java/OpenRewrite support and currently
  depends on `renovatio-core`;
- `LanguageProviderRegistry` routes source-language tools but is not a target
  emitter registry;
- accepted annotated-IR `DATA_INTENT` values are applied directly to a Java
  AST by `AnnotationApplicator`; and
- `MigrationProfiles.EffectiveProfile` already contains the fully resolved
  profile, sorted decision map, sorted applied ids, and profile hash.

F2 must not introduce a dependency cycle to preserve these placements. The
required direction is:

```text
renovatio-profile       renovatio-semantic-ir
          \                 /
           renovatio-shared (target SPI/envelopes)
                    |
              renovatio-core (registry)
               /           \
provider-cobol (projection)  provider-java (JAVA adapter)
```

`renovatio-semantic-ir` depends only on the Java 17 runtime. `renovatio-profile`
does not depend on it. `renovatio-shared` may depend on both domain modules.
`renovatio-core` depends on shared contracts, not concrete emitters. Concrete
providers may depend on core for orchestration; core must not depend on either
provider. The obsolete provider-java→core dependency must be removed if it
would invert this graph.

## 4. Scope and non-goals

F2 delivers:

1. a new `renovatio-semantic-ir` Maven module and its version-1 contracts;
2. a deterministic COBOL-IR/accepted-sidecar projection into that model;
3. shared `TargetModel`, `TargetEmitter`, and `EmittedArtifacts` contracts;
4. a deterministic `TargetEmitterRegistry` owned by core;
5. one `JavaEmitter` adapter in `renovatio-provider-java` around the current
   JavaPoet/template/OpenRewrite behavior; and
6. profile-aware routing for every active production Java generation entry
   point, including plan/apply and MCP/CLI paths that reach those entry points.

F2 expressly does not deliver:

- a Node or Python emitter, placeholder output, or silent Java fallback;
- any profile-driven architecture graph transformation;
- repository/entity selection, transaction inference, SQL restructuring, or
  other fine-grained persistence classification;
- a replacement COBOL parser or a second source-language IR;
- new LLM decisions or automatic acceptance of annotated suggestions;
- changed generation formatting, artifact paths, public annotation bytes, or
  issue-#122 golden outputs; or
- persistence of the semantic or target model.

## 5. Semantic IR v1

### 5.1 Module boundary

The public package is rooted at `org.shark.renovatio.semantic.ir`. The model is
source-language-neutral and target-language-neutral. It may use Java 17 value
types such as records, enums, `String`, numeric primitives, collections, and
`Optional`; “no Java types” in this specification means no Java-*target*
semantics or compiler/generator APIs.

The module must have no compile or test dependency on COBOL modules,
`renovatio-cobol-annotations`, OpenRewrite, JavaPoet, FreeMarker, Mustache,
Spring, JPA, Jackson, or compiler-tree APIs. Its public names and enum values
must not encode a target language. In particular it must expose no Java class
name, Java type name, package, import, annotation, accessor, template, or AST
node.

### 5.2 Root and node surface

The version-1 root is one `SemanticProgram` with this normative information:

| Field | Contract |
|---|---|
| `schemaVersion` | Required literal `"1"`. |
| `id` | Stable id using §5.5 with kind `PROGRAM` and role `program`. |
| `programId` | Non-blank normalized source program identifier. |
| `sourceProvenance` | Required value from §5.4. |
| `types` | Ordered semantic type declarations. |
| `dataIntents` | Ordered storage/cardinality intents. |
| `sideEffects` | Ordered observable state effects. |
| `ioOperations` | Ordered classified I/O operations. |
| `controlFlow` | Required graph, empty rather than null when no edges exist. |
| `unclassifiedDataAccesses` | Ordered explicit residual accesses. |

Every element in those collections is a semantic node and exposes `id`, a
closed `nodeKind`, a non-blank `semanticRole`, a required `SourceSpan`, and
node-specific content. Implementations may split the records into cohesive
types, but may not omit or weaken this information.

The closed v1 kinds are `PROGRAM`, `TYPE`, `DATA_INTENT`, `SIDE_EFFECT`,
`IO_OPERATION`, `CONTROL_FLOW_NODE`, `CONTROL_FLOW_EDGE`, and
`UNCLASSIFIED_DATA_ACCESS`. Adding a kind requires schema version 2.

### 5.3 Required semantic classifications

The version-1 surface must represent at least:

| Area | Required neutral information |
|---|---|
| Semantic type | Stable symbol, `TEXT`, `INTEGER`, `DECIMAL`, `BOOLEAN`, `GROUP`, or `UNKNOWN` kind; signedness, precision, scale, and cardinality when known; containment/reference ids rather than target types. Unknown facts are explicit optional values or `UNKNOWN`, never fabricated defaults. |
| Data intent | Subject node id, `OVERLAPPING_STORAGE` or `DEPENDENT_CARDINALITY`, interpretation, non-empty ordered assumptions, and accepted evidence id. COBOL `REDEFINES` maps to `OVERLAPPING_STORAGE`; accepted `OCCURS DEPENDING ON` maps to `DEPENDENT_CARDINALITY`. |
| Side effect | `STATE_READ`, `STATE_WRITE`, `EXTERNAL_CALL`, or `UNKNOWN`, affected semantic-node ids, and a concise source-derived description. |
| I/O | `FILE`, `DATABASE`, `TERMINAL`, `TRANSACTION`, `MESSAGE`, or `UNKNOWN`; operation verb, resource reference when known, read/write direction, and related side-effect ids. F2 classifies only facts already proven by the source IR. |
| Control flow | Entry node id, ordered nodes, and ordered directed edges with `SEQUENTIAL`, `BRANCH_TRUE`, `BRANCH_FALSE`, `CALL`, `RETURN`, `LOOP`, or `UNKNOWN` kind. Edges refer only to nodes in the same program. |
| Residual access | Subject/resource text, observed operation, reason classification was unsafe, and related evidence ids. Every data access that cannot safely enter the I/O vocabulary appears here; it may not disappear or be guessed. |

Collections are never null. Required strings are non-null and non-blank.
Optional facts use `Optional`/optional primitives or an explicit closed
`UNKNOWN` value, not null. Record constructors make defensive copies and
reject duplicate node ids, duplicate edges, dangling references, invalid
spans, and inconsistent numeric/cardinality bounds.

### 5.4 Source provenance

`SourceProvenance` contains the normalized workspace-relative source path,
lowercase SHA-256 of the exact input bytes, source-language identifier,
dialect when known, and optional parent evidence/hash references. `SourceSpan`
contains that normalized source path and one-based inclusive start/end line
and column coordinates. An unavailable fine-grained location uses the
containing construct's real span; invented `0` coordinates are forbidden.

The COBOL projector must preserve the base-IR hash and accepted sidecar hash
as provenance references when a sidecar contributes an intent. Raw source,
timestamps, absolute workspace roots, model names, and temporary paths are not
identity inputs.

### 5.5 Stable identity and ordering

Every node id is lowercase SHA-256 over this newline-delimited UTF-8
projection:

```text
semantic-ir.v1
<normalized programId>
<nodeKind>
<normalized source path>:<startLine>:<startColumn>:<endLine>:<endColumn>
<semanticRole>
```

All text is Unicode NFC. Program ids and enum values use `Locale.ROOT`; paths
use `/`, are workspace-relative, and contain neither `.` nor `..` segments.
`semanticRole` is a stable source-derived discriminator and is unique for
nodes with the same program, kind, and span. It must not contain a generated
Java name. Re-analysis of unchanged bytes/configuration produces the same ids.

Externally supplied collection order is never trusted. Node collections sort
by id. Control-flow nodes sort by id; edges sort by `(fromId,toId,kind,id)`.
String ids and evidence references sort lexicographically. Assumptions retain
their accepted semantic order because that order is observable in the current
annotation bytes. Maps, if used internally, iterate by sorted key. Two
independent projections of identical inputs compare equal and serialize in the
same order.

### 5.6 COBOL projection

The source adapter belongs outside `renovatio-semantic-ir`, in the COBOL
provider boundary. It projects `CobolIntermediateModel`, its CFG, diagnostics,
and a validated `AnnotatedCobolContext` when present. Only annotations already
eligible under the current base-hash, node-resolution, family, and
`ACCEPTED`-review rules may contribute semantic facts.

The projector is deterministic and fail-closed. Unrepresented safe facts stay
in the source IR; unsafe or unclassified data access becomes an explicit
residual node and existing manual-action diagnostics remain. Proposed,
needs-review, rejected, stale, unresolved, or invalid annotations never alter
the semantic program.

## 6. Target contracts

### 6.1 `TargetModel`

`TargetModel` is an immutable shared envelope containing exactly:

```text
SemanticProgram semanticProgram
MigrationProfile profile
Map<String,String> resolvedDecisions
List<String> appliedDecisionIds
String profileHash
SourceProvenance sourceProvenance
```

It is created directly from one `MigrationProfiles.EffectiveProfile`; values
are copied without re-resolution. Decision keys sort lexicographically,
applied ids sort lexicographically and are unique, and `profileHash` is a
lowercase 64-character SHA-256. The envelope provenance equals the semantic
program provenance. Its target equals `profile.target().language()`.

F2 introduces no target-specific fields and no graph reshaping between the
semantic program and this envelope. Any later normalized/architectural target
graph requires a separately versioned contract.

### 6.2 `TargetEmitter` and artifacts

The shared SPI preserves issue #147's operation:

```java
boolean supports(MigrationProfile.Language target);
EmittedArtifacts emit(TargetModel model, MigrationProfile profile);
```

An emitter declares support deterministically and without I/O. The registry
passes `model.profile()` as the second argument; an emitter rejects a
non-equal profile. Emission does not mutate the model, source tree, profile, or
decision envelope.

`EmittedArtifacts` is an immutable, path-keyed set of binary artifacts. Paths
are unique, normalized, workspace-relative, `/`-separated, and sorted
lexicographically. Content is preserved as exact bytes with defensive copies;
text conversion is explicitly UTF-8. Empty paths, absolute paths, `.`/`..`
segments, duplicate paths, and null content are invalid. The adapter to
`StubResult` preserves the current generated map keys, text, target language,
success semantics, and write order.

### 6.3 `TargetEmitterRegistry`

Core owns a registry built from the complete emitter collection. Construction
enumerates every declared `MigrationProfile.Language` in enum-name order and
indexes each target supported by exactly one emitter.

- Zero emitters for a target is allowed at construction.
- More than one emitter for a target throws a deterministic
  `DuplicateTargetEmitterException` before the registry becomes usable. The
  exception includes the target and the sorted fully qualified emitter class
  names.
- `resolve(target)` returns the sole emitter or throws
  `TargetEmitterUnavailableException` with machine code
  `TARGET_EMITTER_UNAVAILABLE`, `requestedTarget`, and `availableTargets`.
  Available targets are unique and sorted by enum name.
- `emit(model)` derives the requested target only from the effective profile,
  resolves it, and calls `emit(model, model.profile())` exactly once.
- No missing, duplicate, or failing emitter falls back to Java or to a first
  registered implementation.

For F2, `JAVA` resolves to `JavaEmitter`; `NODE` and `PYTHON` produce, exactly:

```json
{
  "code": "TARGET_EMITTER_UNAVAILABLE",
  "requestedTarget": "NODE",
  "availableTargets": ["JAVA"]
}
```

with `requestedTarget` changed to `PYTHON` for that request. API, MCP, and CLI
adapters retain this structured detail; presentation text may be added but may
not replace the machine fields.

## 7. Java adapter and cutover

`JavaEmitter` is owned by `renovatio-provider-java`. JavaPoet, templates,
OpenRewrite, Java compiler types, annotation projection, package/import
selection, and Java formatting remain behind this adapter. None may leak into
semantic IR or `TargetModel`.

The existing output-producing implementation may be moved or decomposed to
break module cycles, but its behavior is retained behind `JavaEmitter`.
`JavaGenerationService` may remain as a compatibility façade only if it builds
the semantic program/target envelope and delegates to the registry; it must no
longer be an alternative emission authority.

Every active production route that emits Java must cross the registry once.
This includes `CobolLanguageProvider.generateStubs`, the
`MigrationPlanService` `GENERATE_JAVA_STUBS` step, and the active copybook,
DB2, and control-break generation tools exposed by `CobolLanguageProvider`.
MCP and CLI must call these governed routes rather than instantiate an emitter
or Java service directly. The non-component sample `CobolProvider` is not a
production authority and must not be registered as a bypass.

For multi-program workspaces, the source adapter constructs one `TargetModel`
per semantic program in the current deterministic program order. The registry
resolves the same effective target for each model. Artifact aggregation rejects
duplicate paths rather than allowing last-write-wins.

## 8. `CobolDataIntent` compatibility projection

The annotation remains
`org.shark.renovatio.cobol.annotations.CobolDataIntent`, with runtime retention,
`FIELD`/`TYPE` targets, and members in the existing declaration order:

```text
nodeId, annotationId, construction, interpretation, assumptions
```

For accepted neutral intents, the Java adapter maps:

| Neutral intent | Annotation construction |
|---|---|
| `OVERLAPPING_STORAGE` | `REDEFINES` |
| `DEPENDENT_CARDINALITY` | `OCCURS_DEPENDING_ON` |

`subjectNodeId` becomes `nodeId`; accepted `evidenceId` becomes
`annotationId`; interpretation and assumptions are copied exactly. Placement
uses the same resolved generated field/type. Annotation application order
remains node id, data-intent priority, then annotation/evidence id. Java
annotation sorting, import insertion, quoting/escaping, braces, commas, spaces,
and line endings remain byte-identical to the baseline.

Current `AnnotationApplicator` callers and consumers must continue to compile
and observe the same result. The implementation may adapt its input behind a
compatibility constructor/factory, but it must not parse generated annotations
back into semantic IR. The neutral semantic intent is the sole emission source
of truth.

## 9. F1 profile integration

Each production request resolves `MigrationProfiles.EffectiveProfile` once.
The resulting `profile`, `resolvedDecisions`, `appliedDecisionIds`, and
`profileHash` are copied into every target model unchanged. No emitter, source
adapter, MCP handler, CLI handler, or provider recomputes defaults, changes
decision precedence, drops unapplied resolved decisions, or accepts a second
target parameter.

The registry selects only `effective.profile().target().language()`. F1's
`TARGET_NOT_ACTIVE` guard is replaced at the emission boundary by the registry
contract for valid but unavailable Node/Python targets. Profile validation
errors remain profile errors and are not relabeled as emitter errors.

Default F1 resolution still produces Java 17, Spring Boot, JavaBeans, and all
seven resolved decisions. Because F1 records non-default generation choices
without applying all of them, F2 must not opportunistically activate new
output behavior beyond the already accepted F1 implementation.

## 10. Failure and compatibility rules

- Projection validation failure emits no artifacts for the affected program
  and retains the existing deterministic diagnostic/action-item behavior.
- Emitter absence and duplicate registration use §6.3; neither is converted
  to an empty successful `StubResult`.
- Emitter failure is propagated without partial aggregation. Existing
  filesystem replacement and action-item safety behavior remains intact.
- Identical source bytes, accepted sidecar, effective profile, schemas, and
  toolchain produce identical semantic ids, target envelope, artifact paths,
  and artifact bytes.
- The issue-#122 expected files and F1 default envelope are the compatibility
  oracle. Updating goldens to make F2 pass is forbidden unless a separate
  reviewed spec explicitly authorizes a semantic change.
- JaCoCo configuration and thresholds remain unchanged. Tests must exercise
  new branches rather than exclude them or lower coverage.

## 11. Required tests and evidence

Implementation planning must map each test to one of the following gates.
Execution evidence records command, revision, exit code, and report/artifact
paths.

| Gate | Required proof |
|---|---|
| Semantic model | Constructor/property tests for null rejection, defensive copies, invariants, stable ids, deterministic order, provenance, dangling-reference rejection, and independent repeat equality. |
| Neutrality | ArchUnit rules proving `renovatio-semantic-ir` has no forbidden dependency or target-specific public type, and proving core has no concrete-provider dependency. |
| COBOL projection | Focused fixtures for semantic types, both data-intent mappings, side effects, each safe I/O class, CFG ordering, residual unclassified access, rejected/stale sidecars, and repeat determinism. |
| Registry | Exactly-one Java resolution; deterministic duplicate startup failure; Node and Python structured errors with sorted `["JAVA"]`; no fallback; profile mismatch; one-call routing; duplicate artifact rejection. |
| Java adapter | All 13 issue-#122 fixture directories exercised through the registry; supported outputs and annotated outputs compare key sets and bytes, not normalized text; repeated independent runs agree. |
| Annotation | Existing `AnnotationApplicator` tests plus exact source-byte assertions for type, member order/values, placement, imports, escaping, assumptions order, and no read-back path. |
| F1 integration | Default and explicit profiles prove the envelope equals `EffectiveProfile`, selection uses only its target, and resolved/applied ids/hash are unchanged. |
| Entrypoints | Provider stubs, plan/apply, copybook, DB2, control-break, MCP, and CLI tests prove registry routing and structured unavailable-target propagation. |
| Reactor | Focused new-module/provider/core tests, functional Maven reactor, MCP server regression suite, and `renovatio-cli` regression suite. |

The verification attempt must include the literal repository-root command:

```text
mvn clean install
```

The plan may also define focused and functional-reactor commands to obtain
actionable evidence. It may not substitute flags into the required literal
attempt. If the literal command fails only because the unchanged baseline
JaCoCo configuration already fails on the rebased F1 merge commit, the report
must include both baseline and F2 command outputs, show the same failure, and
record an explicit baseline exception. That exception does not authorize
threshold changes and does not excuse failures introduced or widened by F2.

## 12. Acceptance traceability

| Agora criterion | Normative sections |
|---|---|
| `semantic-ir` | §§5.1–5.5 define the module, root, vocabulary, invariants, provenance, identity, and neutrality contract. |
| `intent-projection` | §§5.3, 5.6, and 8 make neutral accepted intents authoritative while preserving the exact annotation projection. |
| `emitter-spi` | §6 defines `TargetModel`, `TargetEmitter`, artifacts, selection, duplicate handling, and unavailable-target errors. |
| `java-adapter` | §§7–8 require the Java adapter, full routing cutover, and byte-compatible Java/annotation output. |
| `profile-integration` | §§6.1 and 9 bind target selection and the target envelope to the single F1 effective result. |
| `regression-gates` | §§10–11 define deterministic compatibility, issue-#122, ArchUnit, Maven, MCP, CLI, and JaCoCo evidence. |
| `scope-boundaries` | §§1 and 4 exclude real Node/Python emission, architecture transformation, fine persistence, and unrelated semantic expansion. |

## 13. Later-phase deliverables

Before implementation begins, Agora must register a `plan` that names the
module moves, public types, test-first sequence, exact Maven commands, and
cycle-breaking order. Before completion, it must register a review and a
`test-report` containing every gate in §11, the final F1 merge baseline, and
the exact-byte comparison results. This specification does not claim that
those deliverables or the implementation already exist.
