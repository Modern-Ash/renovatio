# Issue #222 — Baseline test and verification report

Date: 2026-09-08  
Baseline: `origin/main` at `6b46865171a795a18e84c570211c1dea14a7e6ea`  
Environment: Linux 7.0.0-31-generic x86_64, OpenJDK 21.0.12, Maven 3.9.12

## Outcome

The canonical baseline is green. No functional commit was ported from the
historical convergence branches: the audit found that their useful behavior is
already present on `main`, while the remaining MVC/CICS series regresses the
current manifest contract.

## Historical regression reproduction

The historical head `71178f4451fa2616036d5c3a3967da519993ef77`
(`agora/decision-engine-f8`) was tested in a detached, disposable worktree.

The focused Maven execution reproduced the decisive MVC/CICS failure:

- `JavaGenerationRegistryRoutingTest.cicsControllerIsPlannedBeforeManifestValidation`
  failed with `TARGET_MANIFEST_MISMATCH` because
  `RoutedCicsController.java` was emitted outside the declared manifest.

The full offline reactor on that head additionally reproduced:

- `ArchitectureTransformerTest.rejectsInactiveAndDuplicateProfiles`.

This confirms that replaying the five-commit MVC series
`6ee45fcf..3ea483b9` would reintroduce behavior already superseded by the
canonical architecture and manifest pipeline.

## Canonical MVC/CICS contract

The following three guardrails define the accepted contract on `main`:

```text
ArchitectureTransformerTest.supportsLayeredProfilesAndRejectsDuplicateProfiles
JavaArchitectureLayoutPlannerTest.plansConditionalCicsControllerInBothLayouts
JavaGenerationRegistryRoutingTest.cicsControllerIsPlannedBeforeManifestValidation
```

Command:

```bash
mvn -o -pl renovatio-provider-cobol -am \
  '-Dtest=ArchitectureTransformerTest#supportsLayeredProfilesAndRejectsDuplicateProfiles,JavaArchitectureLayoutPlannerTest#plansConditionalCicsControllerInBothLayouts,JavaGenerationRegistryRoutingTest#cicsControllerIsPlannedBeforeManifestValidation' \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dexec.skip=true -Djacoco.skip=true test
```

Result: **3 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS**.

## Full reactor

Command:

```bash
mvn -o -Dexec.skip=true -Djacoco.skip=true test
```

Result: **22/22 modules — BUILD SUCCESS** in 2 minutes 2 seconds.
Surefire produced 173 reports containing **706 tests, 0 failures, 0 errors,
0 skipped**.

`-Dexec.skip=true` suppresses frontend/npm executions in the Java reactor; the
reproducible build and CI bootstrap itself belongs to AC-02 / issue #223. The
Java baseline required by AC-01 is fully green.

## Reconciliation audit

Command:

```bash
./scripts/audit-branch-convergence.sh \
  origin/main \
  refs/remotes/workspace/agora/decision-engine-f8 \
  refs/remotes/workspace/agora/renovatio-workbench-bootstrap \
  refs/remotes/workspace/agora/issue-217-carddemo-coverage
```

Result: exit code 0. The report includes merge bases, raw and patch-equivalent
counts, `git range-diff` classifications, stable patch IDs, and every
candidate-exclusive commit.

## Repository and governance checks

- `git diff --check`: passed.
- Rollback tags were created and published:
  `ac01-pre-convergence-main-20260908`,
  `ac01-pre-convergence-f8-20260908`, and
  `ac01-pre-convergence-workbench-20260908`.
- `agora validate` reports only findings inherited from the starting `main`:
  seven historical `evidence-entry.artifact-changed` errors and three stale
  clarification warnings. It reports no finding owned by issue #222.
- No source code was changed; this work establishes the canonical baseline,
  contract, rollback points, and repeatable audit needed before AC-02 through
  AC-07.

## Verdict

AC-01 is satisfied by evidence-backed reconciliation with zero functional
ports. Blind branch merging is rejected. Future convergence work must start
from current `main`, preserve the shared `DomainModel` / `ArchitectureModel` /
`ArtifactManifest` contract, and pass the MVC/CICS guardrail triad plus the
full Java reactor.
