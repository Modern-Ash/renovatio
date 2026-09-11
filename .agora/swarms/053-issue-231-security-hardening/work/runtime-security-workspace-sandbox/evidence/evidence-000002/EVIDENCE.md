---
schema: "agora/evidence-entry/v3"
id: "evidence-000002"
type: "test"
phase: "api-workspace-sandbox"
result: "success"
revision: 1
artifact-references: ["docs/reports/issue-231-test-report.md"]
artifact-content-sha256: {"docs/reports/issue-231-test-report.md":null}
produced-by: "project:agent"
timestamp: "2026-09-11T15:33:57.984308Z"
tested-commit: null
command: ["mvn -pl renovatio-api -am -Dexec.skip=true -Dtest=ProjectServiceWorkspaceSecurityTest,WorkbenchProjectAdapterServiceTest -Dsurefire.failIfNoSpecifiedTests=false test"]
exit-code: 0
tests-total: 3
tests-passed: 3
tests-failed: 0
environment: "local"
dedupe-key: "issue-231-api-tests"
---

# Evidence evidence-000002

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
