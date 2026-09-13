---
schema: "agora/evidence-entry/v3"
id: "evidence-000002"
type: "test"
phase: "verification"
result: "failure"
revision: 1
artifact-references: ["docs/reports/issue-230-test-report.md"]
artifact-content-sha256: {"docs/reports/issue-230-test-report.md":null}
produced-by: "project:agent"
timestamp: "2026-09-11T14:53:15.254490Z"
tested-commit: null
command: ["mvn -pl renovatio-cli,renovatio-api -am test -DskipITs"]
exit-code: 1
tests-total: 27
tests-passed: 27
tests-failed: 0
environment: "local; API build blocked before tests by missing renovatio-ui vite dependency"
dedupe-key: null
---

# Evidence evidence-000002

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
