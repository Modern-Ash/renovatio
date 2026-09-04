# F8 Implementation Plan — Reusable Profiles and Policies

Method: Agora `spec-driven` + TDD. Governing spec:
`docs/specs/f8-reusable-profiles-policies.md`.

## Phase 1 — Reusable profile domain

- Add immutable `MigrationProfileTemplate`, `TemplateReference`,
  `ProfileTemplateRepository`, and `ProfileTemplateService` contracts to
  `renovatio-profile`.
- Implement canonical immutable-version behavior, deterministic leaf diff, and
  layered profile merge without changing the F1 compatibility overload.
- Add `FileProfileTemplateRepository` with an injectable root, strict identifier
  validation, root containment, symlink rejection, canonical JSON, and atomic
  writes.
- Start with failing tests for A→B reuse, coexistence/idempotency/conflict,
  deterministic listing/diff, traversal, and symlink escape.

## Phase 2 — Policy catalog and matching domain

- Add `SemanticDecisionSignature`, `DecisionPolicyCatalog`,
  `DecisionPolicyEntry`, `PolicyReference`, repository/service ports, match
  result/report, export conflict detection, and threshold validation in
  `renovatio-decisions`.
- Extend decision source/provenance compatibly and add transitions for policy
  auto-confirmation, suggestion, reconciliation, and local override.
- Implement exact and feature-similarity matching with deterministic tie-breaks
  and fail-closed conflicting choices.
- Add `FileDecisionPolicyRepository` with the same storage safety properties as
  templates.
- Test exact/high/medium/low/stale matching, invalid options, v1/v2 binding,
  provenance preservation, deterministic reports, and export conflicts.

## Phase 3 — Layered effective resolution

- Add a `ResolutionLayers` entry point that applies defaults, template,
  policy-derived mappings, project overlay, then explicit project decisions.
- Include exact template/catalog references and applied policy ids in the
  effective content hash while retaining the old F1 method as an empty-binding
  overload.
- Test every precedence boundary and byte/hash compatibility for an unbound
  project.

## Phase 4 — API persistence and endpoints

- Persist explicit project template/catalog bindings and policy provenance;
  keep existing rows readable.
- Wire filesystem repositories as injectable Spring beans and compose them with
  `DecisionLayerService` transactions.
- Add template/catalog list/get/create, project bind/apply/diff, usage lookup,
  and optional project-creation bindings.
- Map invalid input to 400, missing versions to 404, immutable-version conflicts
  to 409, and preserve current access-role checks.
- Add service, repository, MockMvc, and full-lifecycle tests.

## Phase 5 — CLI adapters

- Add `profile` (`save`, `apply`, `diff`, `list`) and `policy` (`export`,
  `apply`, `list`) picocli groups.
- Share domain services/codecs; inject the store root for tests and keep output
  deterministic in human and JSON modes.
- Add command tests for A→B reuse, explicit v1/v2 application, match counts,
  missing/version-conflict errors, and path rejection.

## Phase 6 — React management surfaces

- Extend the API client for templates, catalogs, bindings, diff, and policy
  override metadata.
- Add optional explicit-version selectors to project creation; enrich the
  Decisions step with provenance/staleness and override controls.
- Add a Profiles & Policies page and project binding/diff panel, reusing the
  current visual language with a clear version/provenance rail.
- Preserve semantic headings, labelled controls, 44px touch targets, visible
  focus, responsive grids, reduced-motion behavior, and `aria-live` results.
- Add Vitest/Testing Library coverage for selection, management, provenance,
  override, empty/error/loading, keyboard, and narrow-layout behavior.

## Phase 7 — Integration and compatibility verification

- Run focused tests for `renovatio-profile`, `renovatio-decisions`,
  `renovatio-api`, `renovatio-cli`, and `renovatio-ui` while iterating.
- Run the repository Maven reactor, UI test/build, and issue #122
  characterization harness because resolution feeds generation.
- Produce a criterion-to-test verification report, Agora consistency report,
  and independent review artifact. Fix material findings before completion.

## Criterion traceability

| Criterion | Phases |
|---|---|
| `template-reuse` | 1, 4, 5 |
| `policy-reuse` | 2, 4, 5 |
| `precedence-overrides` | 1, 2, 3, 6 |
| `version-safety` | 1, 2, 4, 5, 6 |
| `ui-management` | 4, 6 |
| `compatibility-quality` | 1–7 |

Implementation begins only after this artifact is registered, every criterion
is marked `planned`, and Agora permits the `implementing` transition.
