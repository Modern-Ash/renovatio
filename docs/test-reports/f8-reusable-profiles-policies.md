# F8 Reusable Profiles and Policies — Verification Report

- **Work item:** `decision-engine-f8/f8-reusable-profiles-policies`
- **Issue:** `Modern-Ash/renovatio#154`
- **Specification:** `docs/specs/f8-reusable-profiles-policies.md`
- **Implementation plan:** `docs/specs/f8-implementation-plan.md`
- **Verified revision:** working tree on `9e39ee1ef478bd309467fa399f77249071559aee`
- **Verification date:** 2026-09-04
- **Overall result:** PASS

## Acceptance results

| Criterion | Result | Inspectable support |
|---|:---:|---|
| `template-reuse` | PASS | Immutable named/versioned templates, canonical hashes, local JSON repository, profile overlay/diff, CLI commands, API routes, and A→B integration coverage |
| `policy-reuse` | PASS | Catalog export, location-independent semantic signatures, confidence thresholds, POLICY application/provenance, CLI/API flows, and A→B seven-decision auto-confirmation coverage |
| `precedence-overrides` | PASS | Shared layered resolver enforces defaults < template < policy < project profile < project decisions; local overrides retain inherited provenance and project linkage |
| `version-safety` | PASS | Explicit bindings, coexisting versions, immutable-version conflict handling, analyzer/schema staleness signaling, and traversal/symlink protections |
| `ui-management` | PASS | Reusable-assets management page, creation and binding flows, project usage, inheritance diff, policy provenance, local override action, responsive/focus-visible styling |
| `compatibility-quality` | PASS | Backward-compatible constructors and empty bindings preserve prior behavior; full reactor, COBOL characterization, focused API/domain/CLI, UI, build, and whitespace gates pass |

## Commands and results

| Command | Result | Observed |
|---|:---:|---|
| `mvn -B -q test` | PASS | 552 Java tests across 155 suites; 0 failures, 0 errors, 0 skipped; UI production build also completed in the reactor |
| `mvn -B -q -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am test` | PASS | Required generation characterization reactor exited 0 |
| `mvn -B -q -pl renovatio-api -am -Dtest=DecisionLayerApiTest,ReusableAssetsApiTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS | Decision-layer and reusable-assets API regression/integration tests exited 0 after the final precedence correction |
| focused profile/decision/CLI suites | PASS | Template persistence/diff, policy matching/versioning/provenance/precedence, command discovery, and A→B CLI workflow passed |
| `npm test -- --run` | PASS | 12 files, 28 tests passed |
| `npm run build` | PASS | Vite production build; 55 modules transformed |
| `git diff --check` | PASS | No whitespace errors |

## Security and compatibility checks

- Asset identifiers are bounded to safe name/version syntax; traversal attempts are rejected.
- Repositories normalize paths, reject symlink paths, verify content hashes, write through a forced temporary file, and atomically publish immutable versions.
- Unknown JSON fields fail closed for stored reusable assets.
- Empty template/policy bindings resolve through the existing default behavior.
- Existing user overrides remain authoritative after re-analysis and after policy reapplication.

## Conclusion

All six acceptance criteria are implemented and verified. No product test failures remain.
