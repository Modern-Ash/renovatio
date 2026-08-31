---
schema: "agora/evidence-entry/v3"
id: "evidence-000001"
type: "unit-tests"
phase: "implementation"
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/idiomatic-polish-proposals-test-report-20260831.md"]
artifact-content-sha256: {"repo://docs/reports/idiomatic-polish-proposals-test-report-20260831.md":"5003b5434ca6f3fc5b930451ecc146173f4c0dfe4e1015e23fd9dbf1d656b92d"}
produced-by: "project:agent"
timestamp: "2026-08-31T13:59:26.438792Z"
tested-commit: "247444eb56d2ce160b5e13e738a65d21e38426bb"
command: ["mvn -q -pl renovatio-provider-cobol -am clean test -o -Djacoco.skip=true -Dtest=PolishContractsTest,IdiomaticPolishServiceTest,PolishSchemaTest -Dsurefire.failIfNoSpecifiedTests=false"]
exit-code: 0
tests-total: 15
tests-passed: 15
tests-failed: 0
environment: "host-java21-release17-offline"
dedupe-key: "issue-128-focused-247444e"
---

# Evidence evidence-000001

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
