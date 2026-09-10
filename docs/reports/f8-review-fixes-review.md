# F8 Post-merge Review Corrections — Review Report

- **Work item:** `decision-engine-f8-review-fixes/f8-review-fixes`
- **Date:** 2026-09-04
- **Verdict:** READY FOR HUMAN ACCEPTANCE

## Review scope

Reviewed the follow-up specification and plan, the five validated findings from PR #167, production changes in `renovatio-profile`, `renovatio-decisions`, `renovatio-core`, and `renovatio-cli`, the added regression tests, and the complete verification results.

## Findings disposition

| Original finding | Disposition |
|---|---|
| Policy application overwrote locally confirmed choices | Fixed by protecting active non-policy final decisions before policy matching |
| Empty reusable bindings changed legacy profile hashes | Fixed by preserving the legacy canonical envelope when bindings are empty |
| CLI profile binding was not consumed by normal execution | Fixed by propagating stable project identity and resolving reusable state inside provider execution |
| CLI policy export depended on test-seeded decisions | Fixed by persisting analysis decisions and adding CLI review/update commands |
| Removed or renamed options could not reach stale-policy handling | Fixed by separating signature compatibility from option-vocabulary freshness |

No unresolved correctness, security, compatibility, or scope findings remain in this follow-up.

## Residual risks and repository notes

- The CLI derives the F1 semantic identity from the stable analysis projection (`data`, `ast`, `symbols`, and `dependencies`). Future IR fields must be added deliberately if they affect decision identity.
- Local CLI storage remains single-project filesystem state, consistent with the existing F8 scope.
- Project-wide `agora validate` still reports inherited historical ledger problems (missing legacy artifacts, non-canonical old session paths, and changed old evidence). None originates in this work item; its own traceability is current.
- Maven reports pre-existing model/deprecation warnings, but all product gates pass.

## Evidence assessment

The complete 20-module reactor, focused domain/CLI/API suites, COBOL characterization contract, UI tests, UI production build, and whitespace check pass. The implementation is traceable to every criterion and is ready for the Spec Owner's final acceptance.
