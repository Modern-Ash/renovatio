# Issue #222 — PR #239 review revalidation

Date: 2026-09-08

Reviewed head before corrections:
`b765b20e2440aeff99cba1010c239fb2c48dc841`.

## Findings

### Exact Maven command

PR comment `discussion_r3963053823` correctly identified that the evidence
record contained the descriptive placeholder `targeted-mvc-cics-triad` as a
positional Maven operand. That value is not a Maven goal and could not replay
the claimed test run.

The evidence now records the exact selector used by the test report:

```bash
mvn -o -pl renovatio-provider-cobol -am \
  '-Dtest=ArchitectureTransformerTest#supportsLayeredProfilesAndRejectsDuplicateProfiles,JavaArchitectureLayoutPlannerTest#plansConditionalCicsControllerInBothLayouts,JavaGenerationRegistryRoutingTest#cicsControllerIsPlannedBeforeManifestValidation' \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dexec.skip=true -Djacoco.skip=true test
```

Revalidation result: **3 tests, 0 failures, 0 errors, 0 skipped; BUILD
SUCCESS** across all 13 selected reactor modules.

### Whitespace verification

PR comment `discussion_r3963053829` correctly identified two Markdown hard
breaks with trailing spaces in the baseline test report. They were replaced
with ordinary paragraph separation. The review also exposed trailing spaces
captured verbatim in the historical branch-publish Tool Result; those were
normalized without changing its provider output.

Command:

```bash
git diff --check 6b46865171a795a18e84c570211c1dea14a7e6ea
```

Revalidation result: **exit code 0, no output**.

### Immutable revision history

Later review passes identified that revision 1 metadata still depended on the
mutable live test report. The exact original bytes are now preserved at:

```text
.agora/swarms/048-architecture-convergence-2026/work/baseline-reconciliation/revisions/0001/snapshot/artifacts/issue-222-baseline-test-report.md
```

Its SHA-256 is
`32e582ca5c8210940dcc721300222bfc7ce509113abb36e195208c28362bd3e8`,
which matches the original revision-1 record. The active latest revision also
carries the MVC test, full reactor, convergence audit, diff check, archive
integrity and CI evidence instead of depending on superseded revisions.

### Range-diff-compatible counts

PR comment `discussion_r3963370644` correctly identified that synthesized
ancestor and descendant summaries used raw commit counts even though
`git range-diff` excludes merge commits. Both branches now use
`git rev-list --no-merges --count`.

The #217 ancestor case therefore retains `0/16` as its raw left/right count
but reports `0/0/11/0` in the range-diff-compatible summary. The reverse
descendant case reports `0/11/0/0`. Both directions were executed against the
preserved refs, and `git diff --check` remained clean.

## Governance disposition

The completed work was reopened through append-only Agora revisions for each
review pass. Every accepted P2 was registered as a structured finding and
resolved with its supporting artifact or evidence. Revision 1 remains
recoverable byte for byte; the current revision carries the complete active
verification set and renewed acceptance. The final evidence-recording commit
is intentionally metadata-only, so CI is bound to the immediately preceding
material head.
