---
schema: "agora/evidence-entry/v3"
id: "evidence-000003"
type: "test"
phase: "verification"
result: "success"
revision: 1
artifact-references: ["docs/reports/issue-230-test-report.md"]
artifact-content-sha256: {"docs/reports/issue-230-test-report.md":null}
produced-by: "project:agent"
timestamp: "2026-09-11T14:59:03.980127Z"
tested-commit: null
command: ["mvn -pl renovatio-application,renovatio-cli,renovatio-mcp-server -am -Dtest=SurfaceCapabilityRegistryTest,CapabilitiesCommandTest,McpToolingServiceCapabilityTest,RenovatioCliSmokeTest -Dsurefire.failIfNoSpecifiedTests=false test"]
exit-code: 0
tests-total: 8
tests-passed: 8
tests-failed: 0
environment: "local"
dedupe-key: "issue-230-java-contract-surfaces"
---

# Evidence evidence-000003

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
