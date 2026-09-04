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
| Surefire aggregate | 542 tests, 0 failures, 0 errors, 0 skipped |
| F7 coverage | 22 tests: 19 JCL, 1 semantic IR, 2 profile |

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
| `defaults-safe` | root `mvn -q test` | PASS — 542 repository tests green |

## PR review regression evidence

`ReviewRegressionTest`, `CondClauseTest`, and `JclParserTest` now cover all twelve
findings from the two review rounds on PR #165:

- passed temporary datasets reuse the same `memory:&&name` identity across steps;
- `DD *` records survive projection and emission as an inspectable inline payload;
- program and utility return codes are persisted for later Spring Batch guards;
- the characterization harness evaluates both THEN and ELSE expressions;
- truth tables cover all valid return codes from 0 through 4095;
- compound `COND` clauses retain and evaluate every referenced step; and
- unnamed DD concatenations attach to the preceding named DD in source order;
- a pending step is flushed when a subsequent JOB card starts;
- procedure-local COND and IF references receive the invocation namespace;
- `COND=ONLY` uses the PRIOR success/abend truth-table entries;
- unsupported SORT transformations are rejected instead of silently ignored; and
- unsupported IDCAMS controls classify as explicit residue.

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

## PR review — rounds 3 to 5 (2026-09-03)

Three further automated review passes on PR #165 (`@codex review` on `9e39ee1e`,
`c6c3cb95`, `8a2ecfb1`). Re-verification commit: `495f8010`. Suite:
`mvn -B test -pl '!renovatio-api'` (the `renovatio-api` module needs an npm
toolchain absent from this environment) — **523 tests, 0 failures, 0 errors,
0 skipped**; `renovatio-jcl` alone: 29 tests incl. `ReviewRegressionTest` (19).

Findings addressed (commits `c6c3cb95`, `0c32dca9`, `8a2ecfb1`, `495f8010`):

| Finding | Resolution |
|---|---|
| Compound `INCLUDE/OMIT COND` matched partially and corrupted the expected value | value restricted to one token or quoted literal; compound clause routes to residue |
| Harness treated an absent RC from a bypassed prior step as "skip" | skip that guard, matching `CondClause.shouldSkip` |
| `SET A=X,AB=Y` corrupted `&AB` into `XB` | longest-name-first substitution with a name terminator |
| `EXEC PROC=…,COND=…` invocation condition dropped | OR-combined into every expanded step (EVEN/ONLY + step COND rejected) |
| `DSN=PROD.INPUT,DISP=(OLD,PASS)` classified as temporary and purged | temporary only for `&&name` or DSN-less PASS allocations |
| `BI` / `PD` SORT keys decoded as display text | rejected as unsupported, routed to residue |
| Nested `IF` blocks overwrote the outer predicate | stack of open IF/ELSE scopes, AND-combined |
| `expandProc` kept a single `nestedIf`, dropping outer/invocation guards | same stacked scope handling inside procedure expansion |
| SORT/MERGE with an out-of-subset control card still classified as a utility | `StepClassifier` parses SYSIN through `SortUtility` → `RESIDUE` on rejection |
| `memory:&&TEMP` shared globally across jobs and concurrent runs | key namespaced as `memory:<jobId>/&&name` |
| `//procstep.ddname DD …` invocation override cards were dropped | buffered on the invocation and applied (replace/add) to the expanded steps |

## PR #166 review — round 7 (2026-09-04)

Re-review after re-targeting `main` (PR #166). Re-verification commit is the tip
of `agora/decision-engine-f7`; suite `mvn -B test -pl '!renovatio-api'` —
**533 tests, 0 failures**; `ReviewRegressionTest` now 26.

| Finding | Resolution |
|---|---|
| Characterization run stopped on the first abending step, so later `COND=EVEN`/`ONLY` were never evaluated | executor exceptions are caught, recorded as a `-1` abend RC; EVEN/ONLY stay eligible, other steps skip; `RunResult.abendedSteps()` added |
| Signed zoned-decimal keys lost their overpunch sign (`01J` parsed as `11`) | `SortUtility` decodes the trailing overpunch character for ZD fields |
| VSAM DDs emitted as bare flat-file DSNs, losing the persistence route | emitted as `vsam:<dsn>` alongside `memory:` for temporaries |
| National characters collapsed to `_`, so `A#B` and `A@B` produced one method | generated step method/bean names include the step ordinal |
| Only the first IDCAMS SYSIN command was validated | every command record is checked; an unsupported command → `RESIDUE` |
| `COND=` matched as a substring of `PARM='COND=…'` | `COND` is read from the top-level EXEC assignment map, not a substring scan |

## Open follow-ups (outside F7 scope)

Recorded here and on PR #166 as deferred; each needs its own work item:

1. **CLI/API pipeline integration** — `renovatio-jcl` ships as a library in F7;
   wiring `.jcl` discovery and emitted-file output into a runtime migration path
   is a successor (F7 spec §3 non-goals).
2. **Spring Batch `COND=EVEN`/`ONLY` failure transitions** — the *emitted* job's
   sequential flow stops on abend before those guards; needs explicit
   `.on("FAILED")` transitions. The characterization harness now honours EVEN/ONLY.
3. **Run the emitted Spring Batch source in the characterization gate** — the
   gate currently exercises a hand-written executor, not compiled emitter output.
4. **Unqualified `COND` vs prior return codes** — current behaviour skips when
   the predicate is true against *any* prior step RC (IBM semantics). A reviewer
   proposed comparing against the maximum prior RC; left for the spec owner.
