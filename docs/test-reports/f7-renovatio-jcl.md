# F7 renovatio-jcl — Test Report

- Work: `decision-engine-f7/f7-renovatio-jcl`
- Baseline: `a02f247e`
- Verification date: 2026-09-03
- Runtime: Java 21, Maven reactor
- Result: **PASS**

## Verification summary

| Check | Result |
|---|---|
| `mvn -q -pl renovatio-jcl -am test` | PASS |
| `mvn -q clean test` | PASS |
| `git diff --check` | PASS |
| Surefire aggregate | 1,057 tests, 0 failures, 0 errors, 0 skipped |
| F7 coverage | 17 tests: 14 JCL, 1 semantic IR, 2 profile |

The full reactor initially exposed two compatibility defects already present in
the F6 baseline: incomplete `MOVE_CORRESPONDING` projection and a Spring Boot 2
auto-configuration registration for the persistence module. The verification
run includes the minimal compatibility fixes and is green from the repository
root.

## Acceptance evidence

| Criterion | Inspectable support | Result |
|---|---|---|
| `jcl-parse` | `JclParserTest`, `F7AcceptanceTest` | PASS — three ordered steps and condition graph |
| `cond-truth-table` | `CondClauseTest` | PASS — GT/GE/EQ/LT/LE/NE plus EVEN/ONLY and multi-predicate OR |
| `spring-batch-emit` | `F7AcceptanceTest` | PASS — deterministic ordered Spring Batch source with guarded tasklets |
| `sort-fixture` | `F7AcceptanceTest` | PASS — `SORT FIELDS` plus `INCLUDE COND` equals the reference records |
| `dd-datasets` | `F7AcceptanceTest`, `BatchJobTest` | PASS — sequential resources mapped and temporary data removed |
| `missing-proc` | `JclParserTest` | PASS — unresolved PROC is recorded without throwing; known PROC expands with override precedence |
| `characterization` | `BatchCharacterizationHarness`, `F7AcceptanceTest` | PASS — output records match reference and temporary dataset is absent |
| `defaults-safe` | root `mvn -q clean test` | PASS — 1,057 repository tests green |

## PR review regression evidence

`ReviewRegressionTest`, `CondClauseTest`, and `JclParserTest` now cover all seven
findings from PR #165:

- passed temporary datasets reuse the same `memory:&&name` identity across steps;
- `DD *` records survive projection and emission as an inspectable inline payload;
- program and utility return codes are persisted for later Spring Batch guards;
- the characterization harness evaluates both THEN and ELSE expressions;
- truth tables cover all valid return codes from 0 through 4095;
- compound `COND` clauses retain and evaluate every referenced step; and
- unnamed DD concatenations attach to the preceding named DD in source order.

## Additional invariants

- Batch IR uses content-stable step/dataset identifiers, immutable collections,
  deterministic ordering, and rejects dangling references.
- Step classification follows migrated program, supported utility, then explicit
  residue precedence.
- `batch.target` defaults to `SPRING_BATCH` for Java; inactive values remain
  storable and unsupported values fail validation at
  `/extensions/batch.target`.
- `BATCH` suggestions use the governed option-only decision service; the
  deterministic fallback remains `RESIDUE` and cannot emit final job code.
- Generated residue steps throw `UnsupportedResidueException`; no unsupported
  work becomes a silent no-op.
