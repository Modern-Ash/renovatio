---
schema: "agora/evidence-entry/v3"
id: "evidence-000001"
type: "test"
phase: "filesystem-sandbox"
result: "success"
revision: 1
artifact-references: ["docs/reports/issue-231-test-report.md"]
artifact-content-sha256: {"docs/reports/issue-231-test-report.md":null}
produced-by: "project:agent"
timestamp: "2026-09-11T15:33:57.930461Z"
tested-commit: null
command: ["mvn -pl renovatio-shared,renovatio-mcp-server -am -Dtest=WorkspaceRootPolicyTest,McpToolingServiceCapabilityTest -Dsurefire.failIfNoSpecifiedTests=false test"]
exit-code: 0
tests-total: 6
tests-passed: 6
tests-failed: 0
environment: "local"
dedupe-key: "issue-231-shared-mcp-tests"
---

# Evidence evidence-000001

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
