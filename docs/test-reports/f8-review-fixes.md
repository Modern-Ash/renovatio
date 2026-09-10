# F8 Post-merge Review Corrections — Verification Report

- **Work item:** `decision-engine-f8-review-fixes/f8-review-fixes`
- **Source review:** PR `Modern-Ash/renovatio#167`
- **Related issues:** `Modern-Ash/renovatio#154`, `Modern-Ash/renovatio#152`
- **Specification:** `docs/specs/f8-review-fixes.md`
- **Implementation plan:** `docs/specs/f8-review-fixes-plan.md`
- **Verified revision:** working tree based on `2ccd4c53`
- **Verification date:** 2026-09-04
- **Overall result:** PASS

## Acceptance results

| Criterion | Result | Inspectable support |
|---|:---:|---|
| `local-confirmation-precedence` | PASS | Policy application now leaves active non-policy `CONFIRMED` and `OVERRIDDEN` decisions unchanged; domain coverage exercises heuristic and LLM confirmations |
| `legacy-hash-compatibility` | PASS | Empty reusable bindings are omitted from the canonical hash envelope and the exact pre-F8 hash is asserted |
| `cli-profile-runtime` | PASS | CLI project identity reaches the provider workspace; the CLI effective-profile resolver reads pinned reusable state for analyze/plan/apply; integration coverage exercises the real Java generation service |
| `cli-policy-export-runtime` | PASS | Successful CLI analysis reconciles and persists F1 decisions; `decisions list/set` provides the normal review path; the A-to-B integration test exports seven confirmed decisions without seeding state |
| `stale-policy-signaling` | PASS | Signature matching tolerates option-vocabulary drift, reports removed/renamed selected options as stale suggestions, and leaves the project decision unchanged |
| `regression-quality` | PASS | Focused domain/CLI/API, complete Maven reactor, COBOL characterization, UI tests/build, and whitespace checks pass |

## Commands and results

| Command | Result | Observed |
|---|:---:|---|
| `mvn -pl renovatio-profile,renovatio-decisions -am test -DskipITs` | PASS | 27 focused profile and decision-domain tests |
| `mvn -pl renovatio-cli -am -Dtest=ReusableCommandsTest,RenovatioCliSmokeTest,LanguageProviderRegistryFullPathsTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS | CLI reuse workflow and command discovery passed; the project-id routing assertion is also covered by the full reactor |
| `mvn -pl renovatio-api -am -Dtest=DecisionLayerApiTest,ReusableAssetsApiTest -Dsurefire.failIfNoSpecifiedTests=false -Dexec.skip=true test` | PASS | 12 focused API tests |
| `mvn -Dexec.skip=true test` | PASS | 20-module reactor; 579 Java tests, 0 failures, 0 errors |
| `mvn -pl renovatio-provider-cobol -am -Dtest=CharacterizationFixtureContractTest -Dsurefire.failIfNoSpecifiedTests=false -Dexec.skip=true test` | PASS | 2 characterization contract tests |
| `npm test` | PASS | 12 files, 28 UI tests |
| `npm run build` | PASS | Vite production build; 55 modules transformed |
| `git diff --check` | PASS | No whitespace errors |

## Compatibility and behavior checks

- Reapplying a reusable policy cannot replace an active locally final decision.
- Unbound projects retain the legacy effective-profile cache identity exactly.
- Profile and policy bindings remain explicit and version-pinned.
- Dry-run apply keeps the original project identity while using its temporary execution workspace.
- A removed policy option remains reviewable as stale but is never written into the decision.
- CLI policy export is proven from analysis-generated, user-confirmed durable state rather than test-only fixtures.

## Conclusion

All six acceptance criteria pass. The five validated post-merge review findings are corrected with regression coverage.
