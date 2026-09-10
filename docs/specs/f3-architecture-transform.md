# F3 Architecture Transformation

- **Work item:** `decision-engine-f3/f3-architecture-transform`
- **GitHub issue:** #148
- **Method:** Agora `spec-driven` with TDD
- **Status:** governed specification
- **Date:** 2026-09-01
- **F2 compatibility baseline:** `b4eeaee1`

## 1. Outcome

F3 inserts a deterministic, target-neutral architecture transformation between
the F2 semantic projection and `TargetEmitter`. It transforms an ordered set of
semantic programs plus the effective migration profile into ordered target
models, an artifact-layout manifest, and an architecture graph. Emitters still
receive the F2 `TargetModel` contract and remain unaware of architecture style.

The phase activates `TRANSACTION_SCRIPT` and `HEXAGONAL` for Java. It also makes
the Target-step preview real: the UI renders the same manifest that emission
uses to choose artifact paths. `LAYERED_MVC`, Node, and Python remain storable
profile values but inactive execution choices in F3.

## 2. Binding clarifications

The Spec Owner accepted these answers on 2026-09-01:

| Question | Binding answer |
|---|---|
| Transformation boundary | `ArchitectureProfile` consumes a project-scoped, deterministically ordered request containing semantic programs and one effective profile. It returns ordered F2-compatible `TargetModel` values plus one neutral architecture graph and one canonical artifact manifest. The `TargetEmitter` SPI is not changed. |
| Grouping precedence | Explicit manual assignments win, then domain-copybook evidence, then the longest matching configured prefix, then one module per program. Conflicts at the same precedence are validation errors; LLM output is never applied implicitly. |
| Unsafe HEXAGONAL control flow | Fallback is per program to `TRANSACTION_SCRIPT`, never project-wide and never silent. The requested style remains in the profile; the result records effective style, diagnostic, evidence and affected program. No unproven port, use case, entity, or relation is invented. |
| Preview and diagram | A canonical backend manifest is the only source of artifact paths for both preview and emission. The accompanying graph contains stable modules, components and relations; the UI renders an accessible tree plus a derived SVG view without storing a hand-drawn diagram. |

Artifact registration, criterion stages, and lifecycle transitions remain
separate durable Agora actions.

## 3. Scope and non-goals

F3 delivers:

1. a new `renovatio-architecture` Maven module;
2. project-scoped `ArchitectureRequest`, `ArchitectureResult`, graph, manifest,
   grouping, diagnostics, and transformation contracts;
3. deterministic `TRANSACTION_SCRIPT` and `HEXAGONAL` transformations;
4. `BY_PROGRAM`, `BY_DOMAIN`, and `SINGLE_MODULE` grouping, including explicit
   manual, copybook, and prefix evidence;
5. optional governed architecture suggestions through the existing F1
   suggestion service and existing control-flow gate;
6. orchestration before target-emitter selection; and
7. API and UI views derived from the canonical result.

F3 does not deliver:

- `LAYERED_MVC`, clean, onion, CQRS, or any other architecture style;
- Node or Python emitters;
- generated tests for destination code;
- fine persistence classification reserved for F4;
- a replacement parser, a second semantic IR, or a refactor of emitter SPI;
- automatic acceptance of LLM suggestions; or
- persistence of generated source as part of preview.

## 4. Module and dependency boundary

The module direction is:

```text
renovatio-semantic-ir   renovatio-profile
          \                /
        renovatio-shared (F2 emission envelope)
                    |
        renovatio-architecture
                    |
      provider/core orchestration -> TargetEmitterRegistry
```

`renovatio-architecture` may depend on semantic IR, profile, and shared neutral
contracts. It must not depend on JavaPoet, OpenRewrite, templates, React,
Spring MVC, a concrete provider, or target-language compiler types. Concrete
emitters and the UI may depend on its public result DTOs only through their
orchestration/API adapters.

The F2 `TargetEmitter` method signatures do not change. Existing constructors
and factories for one-to-one `TargetModel` creation remain source compatible.
Architecture metadata added to the shared envelope is immutable, neutral, and
has an identity default representing the F2 one-program layout.

## 5. Architecture contract

### 5.1 Request

`ArchitectureRequest` contains:

- a non-empty list of `SemanticProgram`, sorted by normalized source path and
  then program id;
- exactly one `MigrationProfiles.EffectiveProfile` shared by the request;
- a validated `GroupingConfiguration`; and
- accepted, content-addressed architecture/control-flow evidence when present.

Duplicate program identities or source paths are rejected. Request identity is
lowercase SHA-256 over the semantic source provenance hashes, effective profile
hash, canonical grouping configuration, and accepted evidence hashes. It
contains no timestamp, workspace absolute path, prompt, or provider response.

### 5.2 Result

`ArchitectureResult` contains:

- request identity and profile hash;
- ordered `ArchitectedProgram` entries with requested/effective style;
- ordered F2-compatible `TargetModel` envelopes;
- one `ArchitectureGraph`;
- one `ArtifactManifest`; and
- ordered diagnostics and accepted suggestion/evidence ids.

Every program appears exactly once. Every graph component and manifest artifact
references an existing program or semantic node. Collections are immutable,
null-free, unique, and sorted by stable id. Repeating a transformation with the
same request produces equal results and byte-identical canonical JSON.

### 5.3 Stable identities

Graph and manifest ids use lowercase SHA-256 over:

```text
architecture.v1
<request identity>
<kind>
<owning module id>
<owning program id or semantic node id>
<normalized role>
```

Artifact paths use `/`, are workspace-relative, contain no empty, `.`, or `..`
segments, and pass the F2 `EmittedArtifact` validation. Duplicate or aliasing
paths fail the whole transformation before any write.

## 6. Module grouping

The F1 profile values have these executable meanings:

| Value | F3 behavior |
|---|---|
| `BY_PROGRAM` | One module per program unless an explicit manual assignment names a shared module. |
| `BY_DOMAIN` | Resolve manual assignment, then domain copybook, then longest configured prefix, then fall back to one module per program. |
| `SINGLE_MODULE` | Place all programs in one configured or deterministic project module; contradictory per-program manual assignments are rejected. |

Configuration lives under the open F1 extension namespace:

```json
{
  "renovatio.architecture": {
    "singleModuleName": "migration",
    "manualModules": { "PAY001": "payments" },
    "domainCopybooks": { "CUSTOMER-REC": "customers" },
    "prefixModules": { "PAY": "payments" }
  }
}
```

Program ids, copybook names, prefixes, and module names are normalized with
Unicode NFC and `Locale.ROOT`. The longest prefix wins; equal-length matches to
different modules fail validation. Manual assignments to unknown programs and
copybook rules unused by any request are reported, not silently discarded.
Stable module ordering is normalized module name then module id.

## 7. Architecture styles

### 7.1 `TRANSACTION_SCRIPT`

Each program becomes one application service within its resolved module.
Structured paragraphs become operations in source/control-flow order. Semantic
types become neutral model components and proven I/O becomes outbound
dependencies. Unknown or irreducible flow is retained as private operations
with an explicit diagnostic/comment marker; behavior is not guessed.

With the F1 default profile and no grouping extensions, the emitted Java paths
and UTF-8 bytes covered by issue #122 remain identical to the accepted F2
baseline. Any unavoidable difference must be isolated, documented in the
verification report, and explicitly accepted before completion.

### 7.2 `HEXAGONAL`

Only source-proven facts produce components:

- externally reachable program entries produce inbound ports and use cases;
- classified file, database, transaction, message, or external-call effects
  produce outbound ports;
- semantic types whose ownership and behavior are proven become entities or
  value components; and
- I/O implementations become outbound adapters.

Component relations preserve semantic/control-flow references. Naming is
deterministic from source symbols plus the effective F1 naming policy.
Unclassified access remains an explicit unresolved dependency, never a
repository or adapter invented by convention.

### 7.3 Control-flow fallback

HEXAGONAL structuring first uses deterministic CFG analysis. An eligible
accepted plan may be sourced through the existing `ControlFlowPlanGate`; it
must carry the current source/profile baseline and green compilation and
characterization evidence. Rejected, stale, incomplete, or low-confidence
plans do not affect the result.

If a program remains unsafe, that program alone uses effective style
`TRANSACTION_SCRIPT`. The result records machine code
`ARCHITECTURE_FALLBACK_UNSAFE_CONTROL_FLOW`, requested/effective styles,
program id, source provenance, and evidence ids. Preview and emission display
and consume the same fallback result.

## 8. Suggestions and inactive choices

Grouping or use-case uncertainty may call `DecisionSuggestionService` only
when the effective profile enables suggestions and the run limit allows it.
Requests use category `ARCHITECTURE`, content-addressed inputs, closed option
sets, and deterministic fallback. Suggestions remain `SUGGESTED` evidence
until a user confirms or overrides them; they never rewrite profile or grouping
configuration directly.

`LAYERED_MVC` is retained in profile schema v1 for compatibility but is not
active in F3. Preview, Plan, or Apply returns
`ARCHITECTURE_STYLE_NOT_ACTIVE` with requested style and active styles sorted
as `["HEXAGONAL", "TRANSACTION_SCRIPT"]`. The Target UI shows it disabled and
labels it as later. Node and Python retain F2's `TARGET_NOT_ACTIVE` /
`TARGET_EMITTER_UNAVAILABLE` behavior and are not used to claim F3 execution
coverage.

## 9. Manifest, API, and UI

`ArtifactManifest` contains stable artifact id, normalized path, component id,
module id, program id, target language, and content role. It is produced before
emission. Emitters receive the corresponding architected `TargetModel` and
must return exactly the manifest paths assigned to that model; missing, extra,
or changed paths fail with `TARGET_MANIFEST_MISMATCH` before persistence.

The API exposes a read-only project architecture preview using the same
analysis/profile resolution and architecture service as Apply. Its response
contains request/profile identity, requested and effective styles, modules,
components, relations, artifacts, diagnostics, and fallback flags. Canonical
ordering is preserved in JSON. Preview performs no source-tree writes.

The Target step replaces the F1 static cards with:

- an accessible artifact tree grouped by module and program;
- an accessible component/relation list;
- a derived SVG architecture view of the same graph; and
- visible diagnostics, including per-program fallback.

The UI does not calculate paths, infer relations, or retain a separate diagram
model. Loading, empty, error, stale-profile, and unavailable-style states are
covered by component tests.

## 10. Orchestration and atomicity

All production generation routes collect the complete deterministic source
set, create one architecture request, validate the complete result and manifest,
then invoke emitters. Artifact aggregation validates every returned path and
manifest match before the first filesystem write. Any transformation,
collision, unavailable style/emitter, or emission failure produces a failed
Apply result with structured detail and leaves no partial generated tree.

## 11. Verification

Completion requires inspectable evidence for:

1. unit tests for request/result validation, stable identity, grouping
   precedence/conflicts, both styles, inactive styles, and fallback;
2. the same multi-program COBOL fixture producing distinct compilable Java
   layouts for `TRANSACTION_SCRIPT` and `HEXAGONAL`;
3. copybook-domain, prefix, manual, per-program, and single-module grouping;
4. API contract and React component tests proving preview paths equal emitted
   paths and the diagram derives from the returned graph;
5. no filesystem writes on preview, collision, transformation failure, or
   emitter failure;
6. architecture tests proving the new module has no provider, UI, target AST,
   prompt, credential, or network dependency;
7. issue-#122 characterization identity for default `TRANSACTION_SCRIPT`, or a
   separately documented and Spec Owner-accepted diff;
8. Maven reactor, API, MCP, CLI, and UI regression suites; and
9. an attempted literal `mvn clean install`, with any unchanged environmental
   baseline failure recorded rather than hidden or used to weaken gates.

## 12. Acceptance mapping

| Agora criterion | Contract sections |
|---|---|
| `architecture-contract` | §§4–5 |
| `transaction-script` | §7.1 |
| `hexagonal` | §§7.2–7.3 |
| `module-grouping` | §6 |
| `suggestions` | §§7.3, 8 |
| `target-views` | §§9–10 |
| `verification-scope` | §§3, 11 |

