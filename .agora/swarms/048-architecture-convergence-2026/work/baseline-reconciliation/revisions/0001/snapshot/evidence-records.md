# Revision 1 individual evidence records

These are the original individual evidence records from completed revision 1.
They are retained here because revision 2 supersedes them; they are historical
records and are not active evidence for revision 2.

## `issue-222-mvc-contract`

```yaml
id: issue-222-mvc-contract
type: test
phase: null
result: success
revision: 1
artifact-references: [repo://.agora/swarms/048-architecture-convergence-2026/work/baseline-reconciliation/revisions/0001/snapshot/artifacts/issue-222-baseline-test-report.md]
artifact-content-sha256: 32e582ca5c8210940dcc721300222bfc7ce509113abb36e195208c28362bd3e8
produced-by: project:agent
timestamp: 2026-09-08T22:58:53.837867Z
tested-commit: 6b46865171a795a18e84c570211c1dea14a7e6ea
command: [mvn, -o, -pl, renovatio-provider-cobol, -am, targeted-mvc-cics-triad, -Dsurefire.failIfNoSpecifiedTests=false, -Dexec.skip=true, -Djacoco.skip=true, test]
exit-code: 0
tests-total: 3
tests-passed: 3
tests-failed: 0
environment: local-jdk21-maven3.9.12
```

## `issue-222-full-reactor`

```yaml
id: issue-222-full-reactor
type: test
phase: null
result: success
revision: 1
artifact-references: [repo://.agora/swarms/048-architecture-convergence-2026/work/baseline-reconciliation/revisions/0001/snapshot/artifacts/issue-222-baseline-test-report.md]
artifact-content-sha256: 32e582ca5c8210940dcc721300222bfc7ce509113abb36e195208c28362bd3e8
produced-by: project:agent
timestamp: 2026-09-08T22:58:54.102434Z
tested-commit: 6b46865171a795a18e84c570211c1dea14a7e6ea
command: [mvn, -o, -Dexec.skip=true, -Djacoco.skip=true, test]
exit-code: 0
tests-total: 706
tests-passed: 706
tests-failed: 0
environment: local-jdk21-maven3.9.12
```
