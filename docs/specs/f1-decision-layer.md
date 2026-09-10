# F1 Decision Layer — Migration Profile, Decisions, LLM Suggestions, API, and Wizard

- **Work item:** `decision-engine-f1/f1-decision-layer`
- **GitHub issue:** #146 (Epic #152)
- **Method:** Agora `spec-driven` with TDD
- **Status:** governed draft; durable lifecycle state is authoritative
- **Date:** 2026-09-01
- **Compatibility baseline:** `main@f66ecea3ca1c7a60a3f215d514d9f43df494f198`

## 1. Outcome

F1 establishes a versioned, project-scoped decision layer in front of the
existing COBOL→Java generation path. A project can store a migration profile,
inspect deterministic or LLM-assisted decision proposals, confirm or override
them, and obtain one content-addressed effective configuration. The wizard
exposes the same contracts before Analyze and Plan.

F1 does not add a target emitter or perform a real architecture
transformation. The existing generator remains the emission authority. With
the default profile and no confirmed overrides, its generated Java file names
and UTF-8 bytes must remain identical to the compatibility baseline.

## 2. Resolved clarifications

| Question | Binding answer |
|---|---|
| Governing artifact | This file, registered as the work item's `spec` artifact, is the single normative F1 contract. The implementation plan and verification report will be separate later-phase artifacts. |
| HTTP contract | §10 defines the five routes, six operations, bodies, responses, filters, authorization, and error classification. |
| Compatibility baseline | F0 revision 2 merged by PR #155 at `main@f66ecea3ca1c7a60a3f215d514d9f43df494f198`, the 13 issue-#122 characterization fixtures, the two service hashes in §3, exact generated-file comparison, the Maven reactor, UI checks, and MCP tests are authoritative. |
| Decision semantics | §§7–9 define the complete model, stable-id projection, statuses, transitions, persistence, resolver precedence, re-analysis, option validation, and bulk confirmation. |
| LLM/UI acceptance | §§11–12 define strict option-bound output validation, failure telemetry, deterministic non-blocking fallback, wizard ordering, and observable interaction tests. |
| Spec Owner confirmation | `project:owner` registered this file as the required `spec` artifact, marked all seven criteria `specified`, and confirmed on 2026-09-01 that every criterion is sufficiently specified for the `spec-clarified` gate. |

## 3. Repository baseline and corrected assumptions

The baseline is a clean checkout of
`f66ecea3ca1c7a60a3f215d514d9f43df494f198`, which contains F0 revision 2.
The inspected production services remain:

| Source | SHA-256 |
|---|---|
| `JavaGenerationService.java` | `8ec5359ece8a48cc0c8891f235c770a9a5ac7dddc6c79e024f581a32361890c3` |
| `MigrationPlanService.java` | `2e44a17db423b8a70d576aeaa89475f1cfe3e24d057e04fb1ece991dcd4803be` |

At this baseline, `ProjectEntity` contains `id`, `name`, `workspacePath`,
`branch`, and timestamps. It does **not** contain `javaOutputPath`,
`javaPackage`, or `javaArchitecture`. Therefore F1 must not claim that the
canonical repository requires migrating columns that do not exist.

F1 adds new profile and decision persistence. For compatibility with an
installation that independently contains any of the three legacy columns, an
idempotent importer may copy their non-null values into the profile overlay:

- `javaOutputPath` → `extensions["renovatio.java.outputPath"]`;
- `javaPackage` → `extensions["renovatio.java.package"]`;
- recognized `javaArchitecture` → `architecture.style`.

Absent columns are a no-op. Unrecognized architecture text is retained under
`extensions["renovatio.legacy.javaArchitecture"]`. F1 never drops legacy
columns; removal is outside this phase.

The importer is an `ApplicationRunner` that executes after Spring and JPA
initialization and before the application is ready. It uses JDBC metadata to
detect the optional columns, runs transactionally per project on every
startup, and only fills destinations absent from the stored overlay; therefore
an explicit profile value always wins and subsequent runs are no-ops.
Architecture input is trimmed, uppercased with `Locale.ROOT`, and whitespace
or hyphen runs become `_`. The recognized mapping is:

| Normalized legacy value | `architecture.style` |
|---|---|
| `TRANSACTION_SCRIPT` | `TRANSACTION_SCRIPT` |
| `LAYERED`, `MVC`, `LAYERED_MVC` | `LAYERED_MVC` |
| `HEXAGONAL`, `PORTS_AND_ADAPTERS` | `HEXAGONAL` |

An unrecognized value is copied to the legacy extension only when that
extension is absent. The importer never overwrites an explicit extension.

## 4. Scope

F1 introduces two Java 17 Maven modules:

- `renovatio-profile`: profile model, canonical JSON/YAML representation,
  schema validation, defaults, overlay merge, and effective-profile hashing;
- `renovatio-decisions`: decision model, stable identity, store contract,
  state transitions, filtering, bulk confirmation, and effective resolver.

It also extends:

- `renovatio-llm` with versioned decision prompts and a bounded
  `DecisionSuggestionService` contract;
- `renovatio-api` with project-scoped persistence adapters and the API in §10;
- `renovatio-ui` with Target and Decisions steps plus API clients;
- Analyze so it persists the seven F0-admitted decision points before the job
  completes.

The module dependency graph must remain acyclic. Domain modules do not depend
on Spring, JPA, the API module, the UI, or a concrete LLM provider.

## 5. `MigrationProfile` v1

### 5.1 JSON shape

The canonical schema resource is `schemas/migration-profile-v1.json`. The
wire and YAML model is:

```json
{
  "schemaVersion": "1",
  "extensions": {},
  "target": {
    "language": "JAVA",
    "languageVersion": "17"
  },
  "architecture": {
    "style": "TRANSACTION_SCRIPT",
    "moduleGrouping": "BY_PROGRAM"
  },
  "runtime": {
    "framework": "SPRING_BOOT"
  },
  "persistence": {
    "defaultStrategy": "IN_MEMORY",
    "transactionBoundary": "METHOD"
  },
  "style": {
    "numericPolicy": "BIGDECIMAL",
    "nullability": "NON_NULL_BY_DEFAULT",
    "errorHandling": "EXCEPTIONS",
    "naming": "JAVA_BEANS"
  },
  "llm": {
    "enabled": false,
    "suggestDecisions": false,
    "maxSuggestionsPerRun": 0
  }
}
```

### 5.2 Closed vocabularies

| Field | Allowed values |
|---|---|
| `target.language` | `JAVA`, `NODE`, `PYTHON` |
| `architecture.style` | `TRANSACTION_SCRIPT`, `LAYERED_MVC`, `HEXAGONAL` |
| `architecture.moduleGrouping` | `BY_PROGRAM`, `BY_DOMAIN`, `SINGLE_MODULE` |
| `runtime.framework` | `SPRING_BOOT`, `NONE` |
| `persistence.defaultStrategy` | `JPA`, `SPRING_DATA_JDBC`, `IN_MEMORY` |
| `persistence.transactionBoundary` | `METHOD`, `PROGRAM`, `NONE` |
| `style.numericPolicy` | `BIGDECIMAL`, `SCALED_LONG` |
| `style.nullability` | `NON_NULL_BY_DEFAULT`, `NULLABLE` |
| `style.errorHandling` | `EXCEPTIONS`, `RESULT_OBJECT` |
| `style.naming` | `JAVA_BEANS`, `FLUENT` |

`schemaVersion` is required and must equal `"1"`. `extensions` is required,
accepts arbitrary JSON values, and is the only open namespace. Unknown fields
outside `extensions` are rejected. `languageVersion` is non-blank and at most
32 characters. `maxSuggestionsPerRun` is an integer from 0 through 100.

All sections are optional in a stored project overlay, except
`schemaVersion` and `extensions`. A canonical serialized full profile contains
every section and uses the field order shown above. YAML and JSON round trips
must preserve the same semantic object; map order does not affect equality or
hashing.

`NODE` and `PYTHON` are valid declared targets so the schema is forward
compatible, but they are inactive in F1. The UI renders them disabled. Any
attempt to Plan or Apply an inactive target fails before emission with
`TARGET_NOT_ACTIVE`; storing or reading such a profile does not create an
emitter.

The complete cross-field validation set for v1 is limited to LLM coherence:

- `llm.suggestDecisions=true` requires `llm.enabled=true` and
  `llm.maxSuggestionsPerRun` from 1 through 100;
- `llm.suggestDecisions=false` requires `llm.maxSuggestionsPerRun=0`.

No other target, architecture, runtime, persistence, or style combination is
rejected at profile-storage time. Unsupported execution combinations remain
storable declarations and fail only at Plan or Apply with
`TARGET_NOT_ACTIVE`, as defined above.

### 5.3 Defaults, overlay, and effective result

Resolution is deterministic:

```text
F1 defaults ∘ stored project profile ∘ CONFIRMED/OVERRIDDEN decision values
```

Object fields merge recursively. A provided scalar replaces the earlier
scalar. Arrays, if introduced under `extensions`, replace rather than append.
Explicit JSON `null` outside `extensions` is invalid. A missing field inherits
the earlier layer.

Only two F1 decision keys override first-class profile fields:

| Decision key/value | Effective profile field/value |
|---|---|
| `java.accessor-convention=JAVA_BEANS` | `style.naming=JAVA_BEANS` |
| `java.accessor-convention=FLUENT` | `style.naming=FLUENT` |
| `java.framework-coupling=SPRING_SERVICE` | `runtime.framework=SPRING_BOOT` |
| `java.framework-coupling=PLAIN_JAVA` | `runtime.framework=NONE` |

The resolver first fully defaults and overlays the stored profile, then applies
those mappings only for active `CONFIRMED` or `OVERRIDDEN` records. The other
five keys do not rewrite profile fields. All seven final machine values remain
present in `resolvedDecisions`, so the hash captures both the effective profile
and the complete decision policy without an implicit mapping.

Because the seven F0 decisions do not all correspond to first-class profile
fields, the effective result is an envelope, not a lossy rewrite of the
profile schema:

```json
{
  "profile": { "schemaVersion": "1" },
  "resolvedDecisions": {
    "java.generated-package": "org.shark.renovatio.generated.cobol"
  },
  "appliedDecisionIds": ["<lowercase sha256>"],
  "profileHash": "<lowercase sha256>"
}
```

`profile` is the fully defaulted profile. `resolvedDecisions` contains every
deterministic default plus confirmed/overridden replacements, ordered by
decision key. `appliedDecisionIds` contains only confirmed or overridden
records, sorted lexicographically. `profileHash` is SHA-256 over canonical
UTF-8 JSON of `profile`, `resolvedDecisions`, and `appliedDecisionIds`; it does
not include timestamps, rationale, confidence, or telemetry.

## 6. Seven F0 decision contracts

F1 emits only the seven decisions selected by F0's strict rule. Their stable
decision keys and baseline defaults are:

| F0 | Key | Category | Baseline default |
|---:|---|---|---|
| #1 | `java.numeric.unscaled-type` | `NUMERIC` | `CURRENT_PIC_MAPPING` |
| #27 | `java.naming.identifier-mapping` | `NAMING` | `CANONICAL_JAVA_IDENTIFIER` |
| #28 | `java.generated-package` | `NAMING` | `org.shark.renovatio.generated.cobol` |
| #30 | `java.accessor-convention` | `NAMING` | `JAVA_BEANS` |
| #33 | `java.framework-coupling` | `ARCHITECTURE` | `SPRING_SERVICE` |
| #37 | `cobol.pic.default-usage` | `NUMERIC` | `DISPLAY` |
| #38 | `java.value-initializer-policy` | `DATA_SHAPE` | `DROP_INITIAL_VALUE` |

The complete F1 option catalog is:

| Key | Ordered machine options |
|---|---|
| `java.numeric.unscaled-type` | `CURRENT_PIC_MAPPING`, `ALWAYS_LONG`, `BIG_INTEGER` |
| `java.naming.identifier-mapping` | `CANONICAL_JAVA_IDENTIFIER`, `PRESERVE_SANITIZED_IDENTIFIER` |
| `java.generated-package` | `org.shark.renovatio.generated.cobol`, `org.shark.renovatio.generated` |
| `java.accessor-convention` | `JAVA_BEANS`, `FLUENT` |
| `java.framework-coupling` | `SPRING_SERVICE`, `PLAIN_JAVA` |
| `cobol.pic.default-usage` | `DISPLAY`, `COMP`, `COMP_3` |
| `java.value-initializer-policy` | `DROP_INITIAL_VALUE`, `FIELD_INITIALIZER`, `CONSTRUCTOR_INITIALIZER` |

The order shown is normative and the first value is the baseline default.
Every emitted `options` list equals its catalog row, has no duplicates, and
uses those stable machine values. F1 records and resolves these values but
does not change emitted Java based on non-default choices. Output-changing
adapters belong to later engine phases.

All seven F1 records are project policies and use the same canonical location:
`programId=project`, `nodeKind=PROJECT`, and `nodeId=project`. F1 emits exactly
one active record per key per project. Consequently `resolvedDecisions` is
unambiguously keyed by `decisionKey`; node-level multiplicity is outside F1
and requires a later, versioned envelope before it can be introduced.

F0 explicitly excluded scaled-decimal/sign policy, statement-level
translation, persistence classification, and architecture transformation.
Those exclusions remain binding.

## 7. `DecisionPoint` v1

The canonical model contains:

| Field | Contract |
|---|---|
| `schemaVersion` | Required literal `"1"`. |
| `id` | Lowercase 64-character SHA-256 from §8. |
| `category` | `NUMERIC`, `CONTROL_FLOW`, `DATA_SHAPE`, `PERSISTENCE`, `NAMING`, or `ARCHITECTURE`. |
| `decisionKey` | One stable machine key; F1 uses the seven keys in §6. |
| `location` | `programId`, `nodeKind`, and canonical `nodeId`; project-wide choices use `nodeKind=PROJECT` and `nodeId=project`. |
| `question` | Non-blank human-facing question; excluded from identity. |
| `options` | Ordered list of 2–20 unique, non-blank machine values. |
| `defaultOption` | Required member of `options`. |
| `chosenOption` | Required member of `options`. |
| `source` | `HEURISTIC`, `LLM`, or `USER`. |
| `confidence` | Decimal in `[0,1]` for the current choice. |
| `rationale` | Non-blank, sanitized, at most 4,000 characters. |
| `evidence` | Ordered references to fixture, IR node, source location, or characterization guard; secrets and raw prompts are forbidden. |
| `status` | `AUTO`, `SUGGESTED`, `CONFIRMED`, or `OVERRIDDEN`. |
| `semanticIrHash` | Lowercase SHA-256 of the analyzed semantic IR; excluded from stable identity. |
| `llmFailed` | Boolean telemetry flag for the current analysis attempt. |
| `llmFailureCategory` | Nullable closed provider/validation failure code; null when `llmFailed=false`. |
| `revision` | Positive optimistic-lock value. |
| `createdAt`, `updatedAt` | UTC timestamps; excluded from identity and profile hash. |

The persistent key is `(projectId, id)`. Projects cannot observe or mutate
each other's profiles or decisions.

`llmFailureCategory` has this complete closed vocabulary and phase mapping:

| Value | Failure mapped to the value |
|---|---|
| `PROVIDER_ERROR` | Provider unavailable, transport error, or provider failure other than timeout. |
| `ATTRIBUTION_ERROR` | Governed attribution gateway rejects or cannot attribute the call after eligibility was established. |
| `TIMEOUT` | Configured provider deadline expires. |
| `MALFORMED_JSON` | Provider output cannot be parsed as one JSON object. |
| `SCHEMA_INVALID` | Parsed output violates the versioned output JSON Schema. |
| `OPTION_INVALID` | `chosenOption` is not an exact member of the supplied options. |
| `SANITIZATION_FAILED` | Rationale cannot be sanitized within the contract. |
| `CACHE_ERROR` | Cache read, identity verification, or cache write fails. |

Validation follows the pipeline order above and records the first observed
failure. A gateway known to be unavailable before eligibility evaluation
causes no attempt and no failure flag; a failure after eligibility maps to
`ATTRIBUTION_ERROR`.

## 8. Stable identity and re-analysis

The decision id is SHA-256 over this newline-delimited UTF-8 projection:

```text
decision-point.v1
<category>
<decisionKey>
<normalized PROGRAM-ID>
<nodeKind>
<canonical nodeId>
```

Normalization uses the existing semantic-IR identity rules: Unicode NFC,
`Locale.ROOT`, canonical program id, closed node kind, and canonical node id.
Question text, option labels/order, rationale, evidence, confidence, source,
status, profile hash, semantic-IR hash, file-system path, and line number are
not identity inputs. Project-wide decisions use the literal location described
in §6.

Re-analysis upserts by `(projectId,id)`. `CONFIRMED` or `OVERRIDDEN` choices
survive when the chosen value is still present in the new option set. If it is
no longer valid, the record resets to the new deterministic default as
`AUTO/HEURISTIC`, sets confidence to the heuristic confidence, and adds a
sanitized `PREVIOUS_CHOICE_INVALIDATED` evidence entry. An absent semantic
location retires the record from active queries without deleting its audit
history. Reappearance with the same id restores it.

## 9. Decision transitions and resolver

### 9.1 Creation and suggestion

- Deterministic analysis creates `AUTO`, `source=HEURISTIC`, and
  `chosenOption=defaultOption` with confidence `1.0` for each of the seven F1
  decisions. Re-analysis invalidation restores that same `1.0` heuristic
  confidence.
- A valid LLM proposal changes an eligible record to `SUGGESTED`,
  `source=LLM`, and its validated option/confidence/rationale.
- An LLM or validation failure retains `AUTO/HEURISTIC/defaultOption`, sets
  `llmFailed=true`, records the failure category, and never fails Analyze.

### 9.2 User mutation

`PATCH` submits exactly one `chosenOption` and the current `revision`.

- If it equals the record's current choice while status is `AUTO` or
  `SUGGESTED`, status becomes `CONFIRMED`; source, confidence, and rationale
  are preserved and revision increments once.
- If it equals the current choice while status is already `CONFIRMED` or
  `OVERRIDDEN`, the operation is an idempotent no-op: status, source,
  confidence, rationale, and revision are preserved. In particular an
  `OVERRIDDEN/USER/1.0` record never becomes confirmed merely because its
  current choice is resubmitted.
- If it differs, status becomes `OVERRIDDEN`, source becomes `USER`,
  confidence becomes `1.0`, and rationale records that a user selected an
  allowed alternative without storing user-entered free text.
- A stale revision returns `409`; a value outside `options` returns `400` and
  leaves the record unchanged.

Bulk confirmation selects active `AUTO` or `SUGGESTED` records with
`confidence >= minConfidence`, ordered by id. It changes only status to
`CONFIRMED`, preserves source and chosen option, and is idempotent. Already
confirmed/overridden, below-threshold, or retired records are counted as
skipped. `minConfidence` is inclusive and must be in `[0,1]`.

### 9.3 Effective precedence

Only `CONFIRMED` and `OVERRIDDEN` records replace decision defaults in the
effective result. `AUTO` and `SUGGESTED` remain reviewable proposals and never
alter Plan or Apply. Among active records, duplicate `(decisionKey,location)`
is an integrity error. Project-level decisions precede node-level decisions;
the latter are keyed by location and do not silently become global defaults.

## 10. HTTP API

There are five routes and six operations. All use `application/json` and the
existing `X-Role` convention. Reads require `canView`; writes require
`canModify` (`ADMIN` or `MANAGER`). Missing/invalid roles return `403`.

### 10.1 Profile

| Operation | Contract |
|---|---|
| `GET /api/projects/{id}/profile` | `200` with the stored normalized overlay and a quoted decimal `ETag`. If none exists, returns `{ "schemaVersion":"1", "extensions":{} }` with `ETag: "0"` without mutating storage. |
| `PUT /api/projects/{id}/profile` | Full replacement by `MigrationProfile` v1 and requires `If-Match` containing the current quoted decimal ETag. Returns `200` with the normalized stored overlay and resulting ETag. An identical PUT is a no-op and preserves the current revision/ETag. |

Malformed JSON or incompatible JSON types return `400`. A well-formed object
that violates `migration-profile-v1.json`, has unknown fields, an unsupported
schema version, invalid ranges, or invalid cross-field semantics returns `422`:

```json
{
  "code": "PROFILE_VALIDATION_FAILED",
  "violations": [
    { "path": "/llm/maxSuggestionsPerRun", "code": "OUT_OF_RANGE", "message": "must be between 0 and 100" }
  ]
}
```

Violations are ordered by JSON Pointer then code. A missing or malformed
`If-Match` returns `400`; project absence returns `404`; an ETag other than the
current revision (including a nonzero token for first creation) returns `409`.

### 10.2 Decisions

`GET /api/projects/{id}/decisions` accepts optional repeated-independent
filters:

- `category`: one category enum;
- `minConfidence`: inclusive decimal `[0,1]`;
- `status`: one status enum.

Invalid query values return `400`. Results are ordered by category,
decisionKey, programId, nodeId, then id:

```json
{ "items": [], "total": 0 }
```

`PATCH /api/projects/{id}/decisions/{did}` accepts:

```json
{ "chosenOption": "JAVA_BEANS", "revision": 3 }
```

It returns the updated `DecisionPoint`. Unknown project/decision returns `404`,
invalid option or body returns `400`, and stale revision returns `409`.

`POST /api/projects/{id}/decisions:bulk-confirm` accepts:

```json
{ "minConfidence": 0.8 }
```

It returns `{ "confirmed": 4, "skipped": 3, "items": [...] }`, with changed
items ordered by id. Invalid threshold returns `400`.

### 10.3 Effective profile

`GET /api/projects/{id}/profile:effective` returns the envelope in §5.3.
Unknown project returns `404`. The same state always produces the same
`profileHash`.

## 11. LLM suggestion contract

F1 adds six versioned prompt ids:

- `decision.numeric.v1`;
- `decision.control-flow.v1`;
- `decision.data-shape.v1`;
- `decision.persistence.v1`;
- `decision.naming.v1`;
- `decision.architecture.v1`.

They share a strict output schema:

```json
{
  "chosenOption": "<member of input options>",
  "confidence": 0.0,
  "rationale": "non-blank sanitized text"
}
```

No additional properties are allowed. In addition to JSON Schema validation,
`chosenOption` must be a member of the exact input `options`, confidence must
be in `[0,1]`, and rationale is sanitized and limited to 4,000 characters.

The canonical cache input contains `semanticIrHash`, `profileHash`, decision
id/key/category/location, ordered options, default option, and sanitized
evidence. `PromptRuntime` contributes prompt version, schema hash, validators,
provider, and model through the existing `CacheIdentity`; no parallel cache
key algorithm is introduced.

Suggestion is attempted only when all conditions hold:

1. `llm.enabled` and `llm.suggestDecisions` are true;
2. heuristic confidence is below the F1 constant `0.8`;
3. the per-run cap has not been reached;
4. a governed attribution gateway is available.

Eligible records are ordered by confidence then id. Cache hits do not consume
the provider-call cap. Provider, attribution, timeout, malformed JSON, schema,
option, sanitizer, or cache failures use deterministic fallback and set
telemetry; they never fail or delay Analyze beyond the configured provider
timeout.

The seven F0-admitted decisions are deterministic high-confidence decisions.
Therefore the default F1 analysis does not call a provider for them. The LLM
service is verified with synthetic low-confidence decision points and remains
available for later phases without widening F1's emitted decision set.

Analyze completion includes counts:

```json
{
  "decisions": {
    "total": 7,
    "suggestionsAttempted": 0,
    "suggestionsFailed": 0,
    "cacheHits": 0
  }
}
```

Raw prompts, provider responses, credentials, and secrets are never returned
by the API or persisted in decision evidence.

## 12. Wizard contract

The step order becomes:

```text
Folder → Target → Analyze → Decisions → Metrics → Plan → Dry Run → Diff → Review → Export
```

### 12.1 Target

Target loads the stored and effective profile. It renders language,
architecture, framework, persistence, and the IA-suggestion toggle. Java is
selected by default; Node and Python are visible but disabled. Architecture
choices update a labeled static layout preview and are explicitly marked as
preview-only until the later architecture phase.

Next performs `PUT /profile`. Schema violations remain on the step and render
the ordered field violations. A successful save places the effective profile
and `profileHash` in wizard state before Analyze.

### 12.2 Decisions

After Analyze completes, Decisions fetches project decisions and supports
category, minimum-confidence, and status filters. Each row displays question,
chosen/default options, confidence, status, and an `IA`, `heuristic`, or `user`
badge derived from source. Rationale and sanitized evidence are expandable.

Confirm sends the current option; edit is restricted to the supplied options;
bulk confirmation defaults to `0.8` and displays confirmed/skipped counts.
Stale revisions refresh the affected row and show a conflict message. Empty
results are a valid state. LLM failure renders the deterministic decision plus
a non-blocking fallback indicator; it never disables Next. Unconfirmed
decisions use defaults and do not require a blanket confirmation to proceed.

Component tests cover render, load, save, validation errors, disabled targets,
preview, filters, badges, rationale, confirm, edit, stale conflict, bulk
confirm, empty state, and LLM fallback. API client tests assert exact paths,
methods, headers, bodies, and error propagation.

## 13. Persistence and lifecycle

Profile storage is one current overlay per project with schema version,
canonical JSON, revision, and timestamps. Decision storage is project scoped,
optimistically locked, queryable by the three API filters, and retains retired
records for audit. Deleting a project cascades its profile and decisions.

Analyze performs deterministic extraction first, persists/upserts all seven
decision points, then attempts only eligible bounded suggestions. A provider
failure cannot roll back deterministic records. Plan and Apply read one
effective snapshot and its `profileHash`; they must not re-resolve midway
through a run.

F1 may pass the effective envelope through a thin adapter to
`JavaGenerationService` and `MigrationPlanService`, but those services must
produce the baseline output regardless of non-default F1 choices. Actual
output-changing decision application is outside F1.

## 14. Acceptance and evidence

### `profile-contract`

- Valid and invalid schema examples cover every field and closed vocabulary.
- JSON and YAML round trips preserve the semantic object.
- Recursive merge and canonical hash fixtures prove defaults < overlay <
  accepted decisions.
- Legacy-column import is idempotent and has an absent-column test.

### `decision-contract`

- Model invariants, all legal transitions, illegal options, stale revisions,
  filter ordering, stable id under textual/path/line changes, invalidation,
  retirement, and bulk threshold/idempotence are tested.
- Two projects with the same decision id remain isolated.

### `llm-suggestions`

- Valid output produces `SUGGESTED` only for an eligible synthetic decision.
- Provider and every validation failure preserve `AUTO/defaultOption`, set
  failure telemetry, and let Analyze complete.
- Cache hit does not call the provider; cap and deterministic ordering hold.
- The seven real F1 decisions make zero provider calls under defaults.

### `api-contract`

- Contract tests cover all six operations, authorization, filters, ordering,
  400/403/404/409/422 semantics, idempotence, and project isolation.

### `ui-workflow`

- Target and Decisions satisfy every observable behavior in §12 through
  Vitest/Testing Library tests modeled after existing wizard tests.

### `compatibility`

The issue-#122 harness runs the 13 characterization fixtures twice: once via
the baseline path and once with the default F1 profile/effective envelope. It
compares the sorted generated-file key set and every generated UTF-8 byte.
Dynamic run ids, timestamps, absolute temporary paths, and elapsed metrics are
excluded; generated source and action-item payload semantics are not.

Required verification commands include:

```text
mvn clean install
mvn -pl renovatio-mcp-server -am test
npm test -- --run
npm run build
```

The final verification report records exact commands, exit codes, test counts,
the tested commit, and the compatibility diff result.

### `scope-boundaries`

Review confirms that no Node/Python emitter, real architecture layout
transformation, data-access classifier, configurable rule engine,
cross-project profile library, or ungoverned LLM mutation was introduced.

## 15. Non-goals

- Applying non-default decisions to generated Java.
- Replacing semantic IR, its identity projector, or characterization fixtures.
- Making LLM output authoritative or requiring LLM availability.
- Dropping legacy database columns.
- Adding user-authored rationale or raw provider-output persistence.
- Implementing F2–F6 behavior from Epic #152.

## 16. Planning constraints

The later implementation plan must preserve TDD order and split work by
contract boundary: profile, decisions, LLM, persistence/API, wizard, then
compatibility integration. It must identify exact module dependencies,
database representation, failure DTOs, prompt resources, and test commands
without weakening any contract in this specification.
