# F3 Architecture Transformation Implementation Plan

- **Work item:** `decision-engine-f3/f3-architecture-transform`
- **Specification:** `docs/specs/f3-architecture-transform.md`
- **GitHub issue:** #148
- **Method:** Agora `spec-driven` + TDD
- **Baseline:** `b4eeaee1`

## 1. Delivery strategy

Implement F3 as a pure architecture kernel followed by adapters. Each slice
starts with failing contract tests, adds the smallest implementation, and runs
the affected Maven/UI tests before the next slice. The existing F2 Java path
remains executable throughout; production routing changes only after identity
`TRANSACTION_SCRIPT` characterization is green.

No slice writes generated files before the complete architecture result,
manifest, and emitted artifact set have passed validation.

## 2. Slice A — neutral contracts and module boundary

### Tests first

- Add constructor/immutability/order/identity tests for architecture request,
  graph, manifest, diagnostics, architected program, and result.
- Add rejection tests for empty requests, duplicate program/source identity,
  dangling graph references, invalid/aliasing artifact paths, duplicate ids,
  inconsistent profile/provenance, and nondeterministic input order.
- Add an ArchUnit/module-boundary test preventing provider, Java AST,
  OpenRewrite, template, Spring, UI, prompt, credential, and network types.
- Extend shared emission contract tests for the backward-compatible identity
  architecture metadata carried by `TargetModel`.

### Implementation

- Add `renovatio-architecture` to the root reactor and dependency management.
- Define immutable v1 contracts under
  `org.shark.renovatio.architecture`.
- Define the minimal target-neutral architecture/manifest slice required in
  `renovatio-shared` so F2 `TargetEmitter` signatures remain unchanged.
- Preserve the existing `TargetModel.from(program, effective)` identity path;
  add an architecture-aware factory rather than breaking current callers.

### Exit check

Focused shared and architecture module tests pass; dependency analysis proves
the pure module boundary.

## 3. Slice B — grouping resolver

### Tests first

- Cover `BY_PROGRAM`, `BY_DOMAIN`, and `SINGLE_MODULE`.
- Cover manual > domain copybook > longest prefix > per-program fallback.
- Cover normalization, stable ordering, unused rules, unknown manual programs,
  equal-precedence conflict, equal-length prefix conflict, and contradictory
  single-module configuration.
- Verify reordered source/config maps yield equal grouping and ids.

### Implementation

- Parse `extensions["renovatio.architecture"]` into a validated immutable
  `GroupingConfiguration`; keep raw extension handling at the adapter edge.
- Implement pure `ModuleGroupingResolver` and structured diagnostics.
- Derive stable module ids and normalized module names without target-language
  package rules.

### Exit check

Grouping tests pass for unit and multi-program fixtures with canonical JSON
snapshots.

## 4. Slice C — architecture profiles

### Tests first

- Characterize identity `TRANSACTION_SCRIPT` model/path behavior before adding
  transformations.
- Verify one service per program, structured paragraph operations, semantic
  model components, and proven outbound dependencies.
- Verify HEXAGONAL inbound ports/use cases, outbound ports/adapters, proven
  entity/value components, stable relations, and explicit unresolved access.
- Verify no entity, repository, port, or relation is produced from unknown
  evidence.
- Verify per-program unsafe-control-flow fallback, mixed-style results,
  diagnostics, and unchanged requested profile style.
- Verify `LAYERED_MVC` produces `ARCHITECTURE_STYLE_NOT_ACTIVE`.

### Implementation

- Introduce `ArchitectureProfile` implementations for
  `TRANSACTION_SCRIPT` and `HEXAGONAL` plus a deterministic registry.
- Implement a project-scoped `ArchitectureTransformer` that groups first,
  transforms every program, validates the aggregate graph/manifest, and then
  creates ordered architecture-aware `TargetModel` envelopes.
- Treat accepted `ControlFlowPlanGate` output as optional validated request
  evidence; keep gate/runtime dependencies outside the pure module.

### Exit check

Both profiles are deterministic, the mixed fallback case is explicit, and the
same fixtures generate two structurally distinct target graphs.

## 5. Slice D — suggestions and orchestration

### Tests first

- Verify architecture suggestions are attempted only when F1 LLM settings and
  limits permit them and use category `ARCHITECTURE` with closed options.
- Verify suggested/rejected/stale/timeout output never mutates grouping or
  target models; only confirmed/overridden, content-addressed evidence enters
  a request.
- Cover plan/apply failure propagation for inactive style, unavailable target,
  transformation failure, manifest mismatch, collision, and emitter failure.
- Cover multi-program collection and prove zero partial writes on every
  aggregate failure.

### Implementation

- Add a thin architecture suggestion coordinator alongside existing F1/LLM
  orchestration; do not add runtime clients to `renovatio-architecture`.
- Insert architecture transformation after complete semantic projection and
  before `TargetEmitterRegistry` selection in every COBOL generation route.
- Validate all emitted paths against manifest slices, aggregate all artifacts,
  and persist once after complete success.

### Exit check

All production entry points consume the same architecture result and retain
structured failures without partial output.

## 6. Slice E — preview API

### Tests first

- Add controller/service contract tests for canonical ordering, profile/request
  identity, graph, manifest, fallback, validation errors, inactive style, and
  workspace/source absence.
- Prove preview calls the same architecture service as Apply and performs no
  source-tree writes.
- Add an integration test comparing preview manifest paths with the emitted
  artifact paths for both active styles.

### Implementation

- Add read-only project architecture-preview DTOs and endpoint in
  `renovatio-api`.
- Reuse effective-profile resolution, deterministic source collection,
  semantic projection, and architecture transformation.
- Map structured domain failures without replacing their machine codes.

### Exit check

API tests prove preview/emission parity and no-write behavior.

## 7. Slice F — Target UI and derived diagram

### Tests first

- Update `StepTarget` tests for async preview loading, style/grouping changes,
  accessible artifact tree, component/relation list, SVG nodes/edges,
  fallback diagnostics, empty/error/stale states, and disabled choices.
- Extend client tests for the exact preview endpoint, request parameters, and
  error propagation.
- Prove rendered artifact labels and relations come from API payload rather
  than static architecture arrays.

### Implementation

- Replace the F1 static preview with a debounced/cancel-safe API-backed preview.
- Render an accessible tree/list and a deterministic SVG projection from the
  returned graph; keep the payload as the single UI model.
- Disable and label `LAYERED_MVC`, Node, and Python consistently with the API.
- Preserve profile optimistic-concurrency behavior when saving and refreshing.

### Exit check

Vitest/component coverage proves the UI is derived from the canonical backend
result and remains keyboard/screen-reader usable.

## 8. Slice G — compilation, characterization, and evidence

- Add fixture compilation tests for both Java layouts.
- Run focused module tests after each slice.
- Run the full affected Maven reactor, API, MCP, CLI, and UI suites.
- Run the issue-#122 offline characterization harness against default
  `TRANSACTION_SCRIPT`; require byte identity or stop for a documented Spec
  Owner decision on a scoped diff.
- Attempt literal `mvn clean install` without weakening JaCoCo or other gates.
- Record commands, counts, hashes, environment constraints, and results in
  `docs/reports/f3-architecture-transform-verification.md`.
- Run architectural isolation checks and inspect the final diff for accidental
  emitter refactor, extra styles, generated target tests, or target-specific
  leakage.

## 9. Criterion coverage

| Agora criterion | Plan slices |
|---|---|
| `architecture-contract` | A, C |
| `transaction-script` | C, D, G |
| `hexagonal` | C, D, G |
| `module-grouping` | B, C, G |
| `suggestions` | C, D |
| `target-views` | D, E, F, G |
| `verification-scope` | A–G |

## 10. Commit and review checkpoints

Prefer independently reviewable governed commits:

1. `feat(architecture): add neutral transformation contracts`
2. `feat(architecture): resolve deterministic module grouping`
3. `feat(architecture): transform transaction and hexagonal models`
4. `feat(decision-engine): route architecture suggestions and emission`
5. `feat(api): expose architecture preview`
6. `feat(ui): render target architecture preview`
7. `test(architecture): record F3 verification evidence`

Before each commit, stage only the intended slice and invoke the governed
`repository/commit` operation. Publish and create the pull request through the
installed Agora Tool Pack operations.

