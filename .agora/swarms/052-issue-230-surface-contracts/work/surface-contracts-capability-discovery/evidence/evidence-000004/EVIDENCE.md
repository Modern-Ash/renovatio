---
schema: "agora/evidence-entry/v3"
id: "evidence-000004"
type: "test"
phase: "verification"
result: "failure"
revision: 1
artifact-references: ["docs/reports/issue-230-test-report.md"]
artifact-content-sha256: {"docs/reports/issue-230-test-report.md":null}
produced-by: "project:agent"
timestamp: "2026-09-11T14:59:11.116111Z"
tested-commit: null
command: ["mvn -pl renovatio-application,renovatio-cli,renovatio-api,renovatio-mcp-server -am -Dtest=SurfaceCapabilityRegistryTest,CapabilitiesCommandTest,CapabilitiesControllerTest,McpToolingServiceCapabilityTest,RenovatioCliSmokeTest -Dsurefire.failIfNoSpecifiedTests=false test"]
exit-code: 127
tests-total: 8
tests-passed: 8
tests-failed: 0
environment: "local; API module invokes renovatio-ui npm build; vite missing because node_modules is absent"
dedupe-key: "issue-230-reactor-vite-missing"
---

# Evidence evidence-000004

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
