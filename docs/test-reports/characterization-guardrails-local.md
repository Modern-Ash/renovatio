# Local Test Report: Characterization Harness and Guardrails

> Agora work: `ai-modernization/characterization-guardrails`  
> Executor: `project:agent`  
> Verification date: 2026-08-31 UTC

## Scope

This report covers the issue #122 implementation present in the governed worktree: the twelve
committed-format characterization fixtures, schema and fallback guardrails, ordered gate runner,
review-eligibility validation, and the pinned network-isolated CI workflow.

The tested implementation is not yet committed. The execution sandbox permits writes to the
workspace but exposes `.git` as read-only, so Git staging failed before a governed repository commit
could be prepared.

## Results

| Check | Result | Detail |
| --- | --- | --- |
| Compilation gate | Passed | All 8 selected reactor projects built successfully with tests skipped. |
| Characterization and affected tests | Passed | 201 tests passed; 0 failed, 0 errored, 0 skipped. |
| COBOL provider tests | Passed | 53 tests passed, including 2 fixture-contract and 3 review-eligibility tests. |
| Twelve-fixture contract | Passed | All declared supported and residual fixture directories satisfied their file and schema contracts. |
| Network-isolated CI | Pending | The workflow is implemented, but this local session did not execute GitHub Actions or the Docker `--network=none` lane. |

## Commands

```text
mvn -B -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am -DskipTests package
mvn -B -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am test
```

Both commands exited with status 0. The test reactor totals were:

- `renovatio-shared`: 23
- `renovatio-core`: 31
- `renovatio-provider-java`: 13
- `renovatio-cobol-runtime`: 23
- `renovatio-cobol-ir`: 55
- `cobol-openrewrite-recipes`: 3
- `renovatio-provider-cobol`: 53

## Remaining gate

An actor or runtime with repository index write authority must stage and commit the implementation
through Agora's governed `repository/commit` operation. The network-disabled workflow must then run
in CI before the `offline-ci` criterion can be verified.
