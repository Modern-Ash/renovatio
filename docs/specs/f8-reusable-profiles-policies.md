# F8 Reusable Profiles and Decision Policies

- **Work item:** `decision-engine-f8/f8-reusable-profiles-policies`
- **GitHub issue:** #154 (Epic #152)
- **Method:** Agora `spec-driven` with TDD
- **Status:** approved governed specification
- **Date:** 2026-09-04
- **Depends on:** F1 (`renovatio-profile`, `renovatio-decisions`, project profile and decision APIs)

## 1. Outcome

F8 lets a user reuse a reviewed migration configuration across similar COBOL
projects without silently coupling those projects. A named, immutable version of
a project profile can seed another project, and a named, immutable policy-catalog
version can reuse confirmed decisions when the new decision point has an
equivalent semantic signature.

Every inheritance result remains inspectable and locally overridable. Resolution
is deterministic and follows this binding precedence:

```text
defaults < template < policy-catalog < project profile < project decisions
```

The first implementation is local-first and single-user. It does not provide a
remote template marketplace, organization RBAC, or automatic propagation from a
new template/catalog version into existing projects.

## 2. Binding clarifications

| Question | Binding answer |
|---|---|
| Storage | The default user store is `${user.home}/.renovatio/profiles/` for templates and `${user.home}/.renovatio/policies/` for catalogs. Tests and embedded deployments may inject a different root. Names and versions are validated opaque identifiers (`[a-zA-Z0-9][a-zA-Z0-9._-]{0,63}`); resolved paths must remain below the configured root. Files are canonical JSON, written atomically. |
| Version semantics | `(name, version)` is immutable. Saving existing identical content is idempotent; different content returns `VERSION_CONFLICT`. Listing is deterministically sorted. Projects bind an explicit name and version and never follow `latest`. |
| Template linkage | Applying a template stores its reference and a project overlay, not a detached full copy. The effective profile is recomputed from the bound immutable template plus the overlay, so local edits preserve linkage. |
| Policy signature | `SemanticDecisionSignature v1` is SHA-256 over canonical JSON containing decision category, decision key, ordered option vocabulary, normalized IR node kind, and analyzer-supplied semantic features. It excludes project id, source path/span, program/node identifiers, timestamps, rationale, and `semanticIrHash`. Project-scoped F1 decisions use node kind `PROJECT` and an empty feature map. |
| Match confidence | Exact v1 signature match has confidence `1.00` and may auto-confirm. A compatible key/category/options match with feature similarity at or above `0.95` may auto-confirm; `0.75..0.95` is only suggested; below `0.75` is ignored. Thresholds are configurable per bound catalog but default to `0.95` and `0.75`, with `auto >= suggest`. |
| Staleness | A policy is stale when its signature schema/analyzer version differs from the decision point or its chosen option is no longer valid. Stale policies never auto-confirm; they are exposed as suggestions with provenance and warning metadata when otherwise compatible. |
| Policy/profile precedence | A policy entry may supply a decision value that maps to a profile field. The resolver applies that mapping to the template layer, then applies the project profile overlay, then applies explicit project `CONFIRMED`/`OVERRIDDEN` decisions. Thus a project profile beats policy and a project decision beats every prior layer. |
| Existing projects | Projects without bindings resolve exactly as in F1. No migration rewrites their stored profile or decision rows. |

## 3. Domain contracts

### 3.1 `MigrationProfileTemplate`

`renovatio-profile` adds an immutable record:

```text
MigrationProfileTemplate {
  schemaVersion = "1",
  name,
  version,
  description?,
  profile,              // validated MigrationProfile overlay
  contentHash,          // SHA-256 of canonical content excluding timestamps
  createdAt
}
```

`ProfileTemplateRepository` provides `save`, `get`, and `list`. The filesystem
adapter is the default CLI adapter; the API may use the same injected store.
`ProfileTemplateService.saveFromProject` snapshots the project's stored profile
overlay. `applyToProject` persists `TemplateBinding(name, version)` and leaves a
project overlay for subsequent local changes. `diff` compares flattened leaf
paths of the effective bound template with the project profile overlay and emits
stable entries `{path, templateValue, projectValue, changeKind}`.

### 3.2 Policy catalog

`renovatio-decisions` adds:

```text
DecisionPolicyCatalog {
  schemaVersion = "1",
  name,
  version,
  signatureSchemaVersion,
  analyzerVersion,
  autoConfirmThreshold,
  suggestThreshold,
  entries[],
  contentHash,
  createdAt
}

DecisionPolicyEntry {
  policyId,
  semanticSignature,
  category,
  decisionKey,
  optionVocabulary,
  chosenOption,
  sourceProjectId,
  sourceDecisionId,
  sourceDecisionRevision
}
```

Export includes only active `CONFIRMED` and `OVERRIDDEN` project decisions. It
deduplicates by semantic signature; conflicting choices cause
`POLICY_CONFLICT` and must be resolved explicitly rather than choosing the most
recent value.

`DecisionPolicyMatcher` returns one result per analyzed decision point:

- `AUTO_CONFIRMED`: compatible, non-stale, confidence at or above the catalog's
  auto-confirm threshold;
- `SUGGESTED`: compatible but below auto-confirm, or stale;
- `UNMATCHED`: no compatible policy.

An auto-confirmed decision is stored with `status=CONFIRMED`, `source=POLICY`,
the catalog name/version, policy id, confidence, and semantic signature. A local
edit uses the existing override transition and changes the source to `USER`
without deleting policy provenance. Applying a catalog returns a deterministic
report with `autoConfirmed`, `suggested`, `unmatched`, and per-decision results.

F8 extends `DecisionPoint.Source` with `POLICY` and adds optional immutable
policy provenance. Existing JSON and database rows without provenance remain
valid.

### 3.3 Effective resolution

The resolver accepts a `ResolutionLayers` envelope and produces the existing
`EffectiveProfile` plus bindings/provenance:

1. F1 defaults;
2. bound template profile;
3. matching policy decisions, including first-class profile mappings;
4. stored project profile overlay;
5. active explicit project decisions.

Maps merge by key, objects merge recursively, and arrays replace. The effective
hash includes the explicit template/catalog references and applied policy ids so
two projects cannot appear equivalent while depending on different versions.
The legacy F1 resolver remains as a compatibility overload with empty bindings.

## 4. CLI contract

The root command gains non-interactive groups:

```text
renovatio profile save <name> --version <version> --project <projectId>
renovatio profile apply <name> --version <version> --project <projectId>
renovatio profile diff <projectId> <name> --version <version> [--json]
renovatio profile list [--json]

renovatio policy export <name> --version <version> --project <projectId>
renovatio policy apply <name> --version <version> --project <projectId> [--json]
renovatio policy list [--json]
```

Names and versions are always explicit for save/apply/export. Human output names
the bound version and summary counts; `--json` emits the domain response only.
Usage errors return 2, missing resources 1, version/path conflicts 1, and success
0, consistent with existing CLI conventions.

## 5. REST API contract

The API exposes equivalent operations:

| Method | Path | Result |
|---|---|---|
| `GET` | `/api/profile-templates` | sorted template version summaries |
| `POST` | `/api/profile-templates` | save from `{name,version,projectId,description?}`; `201` or idempotent `200` |
| `GET` | `/api/profile-templates/{name}/versions/{version}` | immutable template |
| `POST` | `/api/projects/{id}/profile-template` | bind/apply `{name,version}` |
| `GET` | `/api/projects/{id}/profile-template/diff` | diff against its bound version |
| `GET` | `/api/policy-catalogs` | sorted catalog version summaries |
| `POST` | `/api/policy-catalogs` | export from `{name,version,projectId,thresholds?}` |
| `GET` | `/api/policy-catalogs/{name}/versions/{version}` | immutable catalog |
| `POST` | `/api/projects/{id}/policy-catalog` | bind/apply `{name,version}` and return match report |

Project creation accepts optional `profileTemplate: {name,version}` and
`policyCatalog: {name,version}`. Binding is transactional with project creation;
an unknown version yields `404`, invalid identifiers or thresholds `400`, and a
version conflict `409`. Existing project/profile/decision routes remain valid.

## 6. UI contract

- Project creation loads template versions, shows name + explicit version, and
  may start with no template.
- The Decisions step shows a `Policy` badge, catalog/version, confidence, stale
  warning, and an Override action for inherited decisions.
- A Profiles & Policies page lists every name/version, content hash, creation
  time, and projects bound to that exact version; it supports saving/exporting a
  new immutable version and opening its read-only details.
- Project detail shows the active bindings and a profile diff grouped by added,
  changed, and removed leaf paths.
- Empty, loading, error, keyboard focus, and narrow viewport states are tested.

## 7. Safety, determinism, and compatibility

- Identifier validation and post-resolution root containment prevent traversal,
  absolute-path injection, and symlink escape from the configured store.
- Files are written to a same-directory temporary file, fsynced where supported,
  then atomically moved. Readers reject malformed schemas and hash mismatches.
- Canonical serialization, list ordering, matching tie-breaks, and reports are
  deterministic. Multiple equal-confidence policies for one signature with
  different choices fail closed as `POLICY_CONFLICT`.
- Existing projects with no template/catalog binding retain byte-identical F1
  effective-profile output and generation behavior.
- The issue #122 characterization harness must pass because this feature changes
  the inputs used by generation.

## 8. Acceptance criteria and test plan

| Criterion | Verification |
|---|---|
| `template-reuse` | Domain round-trip and path-safety tests; CLI save/apply across projects A/B; MockMvc save/get/bind test; canonical profile equality. |
| `policy-reuse` | Export A, analyze/apply to B, exact/high/medium/low/stale match tests; persistence asserts `CONFIRMED` + `POLICY`; report counts and deterministic ordering. |
| `precedence-overrides` | Layer-by-layer resolver matrix; inherited decision override; leaf diff snapshot; unchanged template binding assertion. |
| `version-safety` | Coexisting v1/v2 fixtures with different choices; explicit binding uses v1 while v2 exists; conflict, stale, and provenance API/UI assertions. |
| `ui-management` | Component/integration tests for project selector, provenance + override, management list/detail/create/export and bound-project usage. |
| `compatibility-quality` | Existing F1 tests; traversal/symlink tests; focused Maven and UI tests; full reactor; issue #122 characterization harness. |

No acceptance criterion is satisfied merely by this document. Each stage,
artifact, test result, review finding, and final acceptance remains a separate
durable Agora action.
