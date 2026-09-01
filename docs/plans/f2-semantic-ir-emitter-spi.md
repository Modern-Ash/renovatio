# F2 Semantic IR and `TargetEmitter` SPI — Implementation Plan

- **Work item:** `decision-engine-f2/f2-semantic-ir-emitter-spi`
- **GitHub issue:** #147 (Epic #152)
- **Specification:** `docs/specs/f2-semantic-ir-emitter-spi.md`
- **F1 baseline:** `152d57462d4d7fc2b4554b6a1f029ae44e96d97a`
- **Method:** Agora `spec-driven`, tests first
- **Plan status:** proposed for governed execution

## Objective

Introduce the neutral semantic domain and target-emission seam without
changing default Java artifact keys or bytes. Break the current module
inversion in small green slices, route every active generation entry point
through one registry, and preserve the F1 effective-profile envelope exactly.

The implementation branch is rebased onto F1 merge commit `152d574`; the
specification, plan, and verification evidence use that committed baseline.

## Dependency cutover

The reactor is changed in this order so it remains acyclic after every commit:

```text
renovatio-profile       renovatio-semantic-ir
          \                 /
           renovatio-shared
                  |
            renovatio-core
             /          \
provider-cobol           provider-java
```

1. Add `renovatio-semantic-ir` before shared in the root reactor and dependency
   management.
2. Add profile and semantic-IR dependencies to shared for the SPI envelopes.
3. Remove provider-java's unused core dependency before provider-cobol gains a
   direct core dependency for target routing.
4. Keep core unaware of concrete providers. Spring supplies emitter beans to
   the registry constructor; provider wiring remains at application edges.
5. Move only Java-target rendering responsibility into provider-java. COBOL
   parsing, validated sidecars, source diagnostics, and semantic projection
   stay in provider-cobol.

ArchUnit tests enforce these edges before production wiring is added.

## TDD delivery sequence

### 1. Semantic domain module

Write failing unit/property tests for:

- schema version, required values, span/path normalization, numeric and
  cardinality invariants;
- defensive copies and immutable returned collections;
- the exact SHA-256 node-id projection and repeatability;
- sorted nodes, edges, ids, and evidence while preserving assumption order;
- duplicate ids/edges and dangling graph/reference rejection; and
- equality of two independently constructed equivalent programs.

Implement the small Java 17 record/enumeration surface under
`org.shark.renovatio.semantic.ir`: provenance/span, node header/identity,
semantic types, neutral data intents, effects, I/O, CFG, residual accesses,
and the program root. Use no serialization framework in the domain.

Add ArchUnit rules proving the module has no source-language, target-language,
Spring, persistence, code-generation, compiler, annotation, or template
dependency. The module POM contains only test dependencies.

### 2. Shared target contracts

Write tests in `renovatio-shared` for:

- exact `EffectiveProfile`→`TargetModel` copying and validation;
- sorted/immutable decisions and applied ids, provenance equality, hash shape,
  and defensive copying;
- immutable binary artifacts, normalized sorted paths, UTF-8 conversion, and
  rejection of duplicates/traversal/absolute paths; and
- the `TargetEmitter` profile-equality precondition.

Implement `TargetModel`, `EmittedArtifact(s)`, and `TargetEmitter` under the
shared SPI/domain packages. Existing `LanguageProvider` remains unchanged;
source-language routing and target-emitter routing are separate concepts.

### 3. Core registry

Start with focused registry tests for:

- exactly one Java emitter and one invocation;
- zero registered emitters;
- Node and Python `TARGET_EMITTER_UNAVAILABLE` details with sorted available
  targets;
- deterministic duplicate errors independent of input collection order;
- one emitter supporting multiple targets;
- null/broken support declarations and no first-emitter/Java fallback; and
- target derivation solely from `model.profile().target().language()`.

Implement a constructor-injected `TargetEmitterRegistry`. Do not repeat the
catch-and-log startup behavior of `LanguageProviderRegistry`: duplicate target
support is a fatal construction error.

### 4. COBOL semantic projection

Add fixture-oriented tests around a new `CobolSemanticProjector` before its
implementation:

- PIC/data items map to neutral type facts without `javaType` leakage;
- both accepted data-intent constructions map to neutral intent kinds and
  retain subject/evidence ids, interpretation, and assumption order;
- proposed, needs-review, rejected, stale, invalid, and unresolved sidecars do
  not alter the semantic program;
- paragraph/statement CFG projects with stable, closed edge kinds;
- known file/DB2/CICS facts classify only to the safe coarse categories;
- unknown access becomes an explicit residual record and retains diagnostics;
  and
- two independent parses/projections have identical ids and ordering.

The projector consumes existing `CobolIntermediateModel` and validated
`AnnotatedCobolContext`. Extend source IR only when a fact required by the
neutral contract is already present in source but otherwise unavailable; do
not add F3 semantics.

### 5. Java adapter extraction

Characterize current Java rendering before moving code. Tests capture artifact
paths and raw UTF-8 bytes for service DTO/interface/implementation output,
copybook, DB2, control-break output, and annotated issue-#122 output.

Then introduce `JavaEmitter` in provider-java. Move target-only name/package,
JavaPoet/template/OpenRewrite, annotation, and formatting code behind it in
mechanical commits. Source parsing and sidecar eligibility remain in the COBOL
assembler. The emitter receives only `TargetModel` plus the equal profile; it
must not import COBOL IR or COBOL provider types.

Where current renderers need source facts, add the corresponding already-known
neutral fact to the version-1 projector contract and its tests. Do not pass
workspace/query/source-provider objects, callbacks, thread-local state, or a
pre-generated Java map through `TargetModel`.

Retain `JavaGenerationService` only as a compatibility façade while callers
are cut over. Its final implementation assembles semantic target models,
delegates once to the registry, adapts `EmittedArtifacts` to `StubResult`, and
contains no renderer.

### 6. Data-intent projection compatibility

Run existing `AnnotationApplicator` tests unchanged first. Add exact-byte tests
covering annotation FQN/import, target placement, member order and values,
construction mapping, assumption order, escaping, whitespace, and line
endings.

Adapt the Java-side annotation projection to consume neutral `DataIntent`
records. Preserve existing constructors/factories as compatibility adapters
where tests or downstream callers require them, but ensure those adapters
project to neutral intent once; no generated annotation is read back into IR.

### 7. Production routing and F1 envelope

Write interaction tests before each cutover:

- `CobolLanguageProvider.generateStubs` and the plan/apply Java step resolve
  through the registry exactly once;
- copybook, DB2, and control-break generation use the same route;
- MCP and CLI reach provider routes and retain structured unavailable-target
  fields;
- multi-program aggregation is deterministic and rejects duplicate paths; and
- default/non-default effective profiles reach every target model with exact
  profile, decision map, applied ids, and hash values.

Replace F1 `TARGET_NOT_ACTIVE` only at emission boundaries. Stored Node/Python
profiles remain valid and now fail with the registry error. No request/query
target parameter can override the effective profile.

### 8. Compatibility and full regression

Extend `CharacterizationFixtureContractTest` so all 13 issue-#122 fixture
directories traverse semantic projection, target-model creation, registry
selection, and Java emission. For each supported output compare sorted path
sets and exact bytes across baseline, F1 default, and F2 registry routes; run
each independently twice.

Run focused modules after every slice, then the functional reactor, MCP, CLI,
and literal root install. Record commands, revision, tool versions, exit codes,
test counts, report paths, and byte comparison. Do not edit fixture goldens or
JaCoCo thresholds to obtain green results.

## Criterion traceability

| Criterion | Planned slices | Required evidence |
|---|---|---|
| `semantic-ir` | 1, 4 | Domain invariants/identity tests, projector fixtures, ArchUnit neutrality |
| `intent-projection` | 4, 6 | Eligibility matrix and exact annotation-byte tests |
| `emitter-spi` | 2–3 | Envelope/artifact tests and complete registry error/duplicate matrix |
| `java-adapter` | 5–8 | Renderer characterization, registry routing, issue-#122 byte report |
| `profile-integration` | 2, 7 | Exact effective-envelope and target-selection tests |
| `regression-gates` | 8 | Focused, reactor, MCP, CLI, literal install, and baseline comparison logs |
| `scope-boundaries` | 1–8 | Dependency/source review showing no emitters/transforms/persistence expansion |

All seven criteria advance to `planned` when this artifact is registered.
Implementation starts only after the governed `clarified → planned →
implementing` transitions.

## Verification commands

The implementation may refine focused test selectors, but the final evidence
must include at least:

```text
mvn -pl renovatio-semantic-ir test
mvn -pl renovatio-shared,renovatio-core -am test
mvn -pl renovatio-provider-java,renovatio-provider-cobol -am test
mvn -pl renovatio-provider-cobol -am -Dtest=CharacterizationFixtureContractTest test
mvn -pl renovatio-mcp-server -am test
mvn -pl renovatio-cli -am test
mvn clean install
git diff --check
```

If Maven's module-specific `-Dtest` propagation causes dependency modules with
no matching test to fail, the focused command may add
`-Dsurefire.failIfNoSpecifiedTests=false`; this does not alter the required
literal `mvn clean install` attempt.

## Commit and rollback strategy

Commit governed Conventional Commits at green boundaries:

1. `feat(semantic-ir)` domain and shared contracts;
2. `feat(core)` registry and errors;
3. `feat(cobol)` neutral projection;
4. `refactor(java)` emitter extraction and annotation compatibility;
5. `refactor(cobol)` production routing and profile integration; and
6. `test(decision-engine)` verification evidence.

Do not commit Maven targets, generated workspaces, local databases, caches, or
credentials. Because routing changes atomically behind interfaces, rollback is
the revert of the last green boundary; no database migration or stored-profile
rollback is required.

## Review focus

Review must inspect module edges, public target contracts, stable-id inputs,
all bypass searches, exact annotation/output diffs, structured error mapping,
and the rebased F1 baseline. A green normalized-text comparison is
insufficient; evidence must compare byte arrays and artifact keys.
