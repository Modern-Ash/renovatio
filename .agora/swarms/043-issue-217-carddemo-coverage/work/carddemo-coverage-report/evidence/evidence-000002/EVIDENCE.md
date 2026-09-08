---
schema: "agora/evidence-entry/v3"
id: "evidence-000002"
type: "report"
phase: "verifying"
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/carddemo-coverage.md"]
artifact-content-sha256: {"repo://docs/reports/carddemo-coverage.md":"a4bc6cb46cbc965aa6201943aeaa61a0d62d0a789ce6fe1a28af130f960031d0"}
produced-by: "project:agent"
timestamp: "2026-09-08T02:35:05.247108Z"
tested-commit: "777ad543ddd741a335d42e630c81de89e2ba9cfa"
command: ["mvn -o test -pl renovatio-provider-cobol -Dgroups=coverage -Drenovatio.surefire.excludedGroups= -Dexec.skip=true"]
exit-code: 0
tests-total: 1
tests-passed: 1
tests-failed: 0
environment: null
dedupe-key: "issue-217-coverage-report-generated"
---

# Evidence evidence-000002

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
