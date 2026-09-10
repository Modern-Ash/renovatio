# F1 Decision Layer — Implementation Plan

- **Work item:** `decision-engine-f1/f1-decision-layer`
- **GitHub issue:** #146 (Epic #152)
- **Specification:** `docs/specs/f1-decision-layer.md`
- **Baseline:** `main@f66ecea3ca1c7a60a3f215d514d9f43df494f198`
- **Method:** Agora `spec-driven`, tests first
- **Plan status:** proposed for Spec Owner coverage confirmation

## Objective

Deliver the versioned profile and decision contracts, bounded suggestion
runtime, project-scoped API persistence, and Target/Decisions wizard steps
defined by the specification. Keep the current COBOL→Java emitter authoritative
and prove that the default effective configuration leaves its generated file
set and UTF-8 bytes unchanged.

## Dependency and ownership boundaries

The Maven reactor gains two leaf-oriented domain modules and these explicit
edges:

```text
renovatio-profile
        ↑
renovatio-decisions
        ↑            ↑
renovatio-llm        │
        ↑            │
        └──── renovatio-api ──── renovatio-provider-cobol
```

- `renovatio-profile` owns `MigrationProfile`, schema/default loading,
  validation, overlay merge, canonical serialization, and effective-profile
  hash inputs. It depends only on Jackson JSON/YAML and a JSON Schema validator.
- `renovatio-decisions` owns `DecisionPoint`, closed enums, identity,
  transitions, store ports, filtering, catalogs, and the resolver. It depends
  on `renovatio-profile` and Jackson; it has no Spring/JPA/provider dependency.
- `renovatio-llm` adds the suggestion adapter and depends on
  `renovatio-decisions`. It continues to own provider, prompt, cache,
  attribution, validation, fallback, and sanitization mechanics.
- `renovatio-api` depends on both domain modules and `renovatio-llm`; it owns
  JPA/JDBC adapters, orchestration, DTOs, HTTP error mapping, and job wiring.
- `renovatio-provider-cobol` remains unaware of profiles and decisions. The API
  decorates Analyze results and passes an inert effective snapshot beside the
  existing Plan/Apply call boundary, preventing a dependency cycle or emitter
  behavior change.
- `renovatio-ui` consumes only the five project-scoped HTTP routes.

The root `pom.xml` adds the two modules before `renovatio-llm` and their managed
dependencies. No domain module imports Spring, Jakarta Persistence, API, UI,
or a concrete LLM provider.

## TDD delivery sequence

### 1. Profile contract

Write failing tests in `renovatio-profile` before production classes:

1. Validate the full default profile and one partial overlay against
   `schemas/migration-profile-v1.json`.
2. Reject every unknown field, enum/range violation, explicit null, unsupported
   schema version, and the complete LLM cross-field invalid set.
3. Prove JSON/YAML semantic round trips, deterministic field ordering,
   recursive object merge, replacing extension arrays, and non-mutating input.
4. Prove canonical UTF-8 hashing is insensitive to map insertion order and
   changes for every normative field.

Then implement immutable Java 17 records/enums, `MigrationProfileDefaults`,
`MigrationProfileValidator`, `MigrationProfileCodec`, `ProfileOverlayMerger`,
and `EffectiveProfileHasher`. Reuse the repository's accepted canonical JSON
behavior rather than creating a locale- or platform-dependent serializer.

### 2. Decision contract

Write failing tests in `renovatio-decisions` for:

1. all model invariants and the exact seven-key ordered option catalog;
2. SHA-256 identity at canonical project location, including invariance under
   question, path, line, rationale, evidence, and option-order changes;
3. `AUTO/SUGGESTED/CONFIRMED/OVERRIDDEN` transitions, invalid options, stale
   revisions, exact idempotent PATCH behavior, and confidence `1.0` defaults;
4. re-analysis preservation, invalidation, retirement/reappearance, ordering,
   filters, bulk threshold/idempotence, and project isolation;
5. resolver precedence and the exact accessor/framework-to-profile mappings,
   sorted resolved decisions, applied ids, and stable `profileHash`.

Implement pure domain services behind `ProfileStore` and `DecisionStore`
ports. Use one `F1DecisionCatalog` to construct exactly seven project-policy
records and one `DecisionIdentity` projector; do not duplicate catalog strings
in API or UI code.

### 3. Persistence and legacy import

Add JPA adapters in `renovatio-api` with H2 integration tests:

- `project_profiles`: `project_id` primary/foreign key, `schema_version`,
  canonical `overlay_json` CLOB, `revision` (`@Version`), and UTC timestamps;
- `project_decisions`: embedded key `(project_id, decision_id)`, category,
  decision key, canonical location columns, question, options/default/chosen,
  source, confidence, rationale, evidence JSON, status, semantic IR hash, LLM
  telemetry, `active`, optimistic `revision`, and UTC timestamps;
- indexes on `(project_id, active, category)`, `(project_id, active, status)`,
  and `(project_id, active, confidence)`; project deletion cascades explicitly
  through repositories/service orchestration.

Use canonical JSON for list/evidence fields so rows round-trip without relying
on database-specific array types. Repository queries enforce project id and
the normative sort in memory only after project-scoped selection.

Implement `LegacyProjectProfileImporter` as the specified `ApplicationRunner`
using `JdbcTemplate`/JDBC metadata. Integration fixtures cover absent columns,
recognized and unknown values, explicit-profile precedence, and repeated
startup execution. F1 does not add Flyway/Liquibase or drop columns because the
current API uses JPA `ddl-auto`.

### 4. HTTP contract

Add `ProjectProfileController` and `ProjectDecisionController`, application
services, and MockMvc contract tests before endpoint implementations.

- Profile GET/PUT emit and consume the exact profile object, quoted decimal
  `ETag`, and required `If-Match`; identical PUT preserves revision.
- Decisions GET accepts only the three specified filters and normative order.
- PATCH and bulk-confirm delegate transitions to the domain layer and expose
  optimistic conflicts without partial writes.
- Effective GET resolves one transactional snapshot.
- Every read checks `canView`; every write checks `canModify`.

Introduce one `ApiProblemDto(code,message)` for 400/403/404/409 errors and
`ProfileValidationProblemDto(code,violations)` for 422. A controller advice
maps malformed JSON/types and headers to 400, missing resources to 404, stale
revisions/ETags to 409, and ordered schema/cross-field violations to 422.
Tests assert exact routes, methods, headers, bodies, ordering, role matrix,
project isolation, and no mutation on any rejection.

### 5. Bounded LLM suggestions

Extend `renovatio-llm` tests first, using `OfflineFakeProvider`, in-memory cache,
and fake attribution:

1. register six prompt resources in `prompts/catalog-v1.yaml`, each pointing to
   one shared strict `schemas/decision-suggestion.v1.schema.json`;
2. validate exact option membership after JSON Schema and sanitize rationale;
3. construct the canonical cache input from every field required by the spec;
4. classify the eight closed failure categories at the first failing phase;
5. prove deterministic fallback, provider-call cap, confidence/id ordering,
   cache-hit behavior, and secret/raw-output exclusion;
6. prove the real seven-record catalog performs zero provider calls.

Implement `DecisionSuggestionService` as a thin orchestration layer over
`PromptRuntime`, `CacheIdentity`, existing cache interfaces, attribution,
provider, and sanitizer. Do not fork the existing cache-key or governed
miss-path implementation. An unavailable gateway before eligibility skips the
attempt; any attempted failure returns the original heuristic decision with
telemetry instead of throwing into Analyze.

### 6. Analyze, Plan, and Apply integration

Add API service tests around `JobService.executeAnalyze` before wiring:

- create/upsert the seven deterministic records only after successful COBOL
  analysis and before job completion;
- retire missing records, preserve valid user choices, and invalidate choices
  removed from the catalog;
- run only bounded eligible suggestions after deterministic persistence;
- add the four decision counters to both persisted job result and SSE
  completion payload;
- keep a provider failure non-fatal and the persisted deterministic records
  transactionally visible.

`EffectiveProfileService` takes one transactional snapshot for Plan/Apply,
rejects inactive targets before invoking generation, and exposes its hash to
job metadata. The provider services receive no output-changing branch in F1;
the adapter proves that default and non-default envelopes call the same
existing generator/planner methods.

### 7. Target and Decisions UI

Write API client tests and Testing Library component tests before components:

- add `getProfile`, `putProfile`, `getEffectiveProfile`, `getDecisions`,
  `patchDecision`, and `bulkConfirmDecisions`, including ETag retention and
  structured error propagation;
- add `StepTarget.jsx` with default Java, disabled Node/Python, architecture
  preview, LLM controls, save validation, and effective state/hash propagation;
- add `StepDecisions.jsx` with filters, badges, expandable rationale/evidence,
  allowed-option editing, confirm, conflict refresh, bulk counts, empty state,
  and non-blocking fallback indicator;
- update `Wizard.jsx` to the normative ten-step order and preserve state/back
  navigation across both inserted steps;
- update Analyze completion parsing for decision counts and next-step label.

Use the existing Tailwind/component vocabulary and accessible labels, native
controls, focus states, and live status/error regions. Do not introduce a new
UI framework.

### 8. Compatibility and regression evidence

Extend the issue-#122 characterization harness rather than creating a parallel
fixture corpus. For all 13 fixtures:

1. run the baseline generation path;
2. run through the default profile/effective-envelope adapter;
3. compare sorted generated-file keys and every UTF-8 byte;
4. run each path twice to detect nondeterminism;
5. exclude only the dynamic metadata listed in the specification.

Also assert that both inspected service source hashes remain unchanged unless
an implementation need is documented and the byte comparison still passes.
No expected output is rewritten to accommodate F1.

## Criterion traceability

| Criterion | Planned phases | Required evidence |
|---|---|---|
| `profile-contract` | 1, 3–4 | Profile unit/schema/hash tests, persistence and HTTP tests |
| `decision-contract` | 2–4, 6 | Domain transition/identity/resolver tests and JPA isolation tests |
| `llm-suggestions` | 5–6 | Prompt, validator, cache/cap/fallback tests and zero-call real catalog test |
| `api-contract` | 3–4 | MockMvc matrix for all six operations and error classes |
| `ui-workflow` | 7 | Client, Target, Decisions, and wizard-order component tests |
| `compatibility` | 6, 8 | 13-fixture file-set/byte report plus reactor, MCP, and UI results |
| `scope-boundaries` | 1–8 | Dependency/source review proving excluded emitters and transforms are absent |

The Spec Owner marks all seven criteria `planned` only after this artifact is
registered. The Developer then transitions `clarified → planned →
implementing`; no production implementation begins before the plan gate.

## Verification commands

Focused tests run after each red/green slice. The final governed evidence uses:

```text
mvn -pl renovatio-profile test
mvn -pl renovatio-decisions -am test
mvn -pl renovatio-llm -am test
mvn -pl renovatio-api -am test
mvn clean install
mvn -pl renovatio-mcp-server -am test
cd renovatio-ui && npm test -- --run
cd renovatio-ui && npm run build
git diff --check
```

The final report records the tested commit, Java/Node versions, exact commands,
exit codes, test counts, compatibility hashes/diff, and any unrelated baseline
warning separately from F1 failures.

## Commit and rollback strategy

Land governed Conventional Commits at coherent green boundaries: module
contracts, persistence/API/LLM integration, UI, and verification records.
Never commit generated caches, credentials, raw prompts, provider responses,
H2 files, `node_modules`, or build output.

The increment is additive. If integration must be disabled, remove API/UI
wiring while preserving stored profiles/decisions; the existing generator and
planner remain callable unchanged. Database rollback never drops the additive
tables or legacy columns automatically. An LLM outage always falls back to the
deterministic seven-decision set and requires no data rollback.

## Non-goals

- Applying non-default decisions to generated Java.
- Adding Node/Python emitters or real architecture transformations.
- Adding a migration framework solely for F1.
- Generalizing node-level decision multiplicity before a versioned envelope.
- Changing issue-#122 fixture expectations or bypassing governed attribution.
