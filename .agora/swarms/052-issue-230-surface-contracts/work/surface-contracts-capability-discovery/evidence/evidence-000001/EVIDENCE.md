---
schema: "agora/evidence-entry/v3"
id: "evidence-000001"
type: "test"
phase: "verification"
result: "success"
revision: 1
artifact-references: ["docs/reports/issue-230-test-report.md"]
artifact-content-sha256: {"docs/reports/issue-230-test-report.md":null}
produced-by: "project:agent"
timestamp: "2026-09-11T14:53:15.222170Z"
tested-commit: null
command: ["mvn -pl renovatio-api -am -Dexec.skip=true -Dtest=CapabilitiesControllerTest -Dsurefire.failIfNoSpecifiedTests=false test"]
exit-code: 0
tests-total: 1
tests-passed: 1
tests-failed: 0
environment: "local"
dedupe-key: null
---

# Evidence evidence-000001

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
