---
schema: "agora/evidence-entry/v3"
id: "evidence-000002"
type: "offline-characterization"
phase: "verification"
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/idiomatic-polish-proposals-test-report-20260831.md"]
artifact-content-sha256: {"repo://docs/reports/idiomatic-polish-proposals-test-report-20260831.md":"5003b5434ca6f3fc5b930451ecc146173f4c0dfe4e1015e23fd9dbf1d656b92d"}
produced-by: "project:agent"
timestamp: "2026-08-31T13:59:26.668011Z"
tested-commit: "247444eb56d2ce160b5e13e738a65d21e38426bb"
command: ["docker --network none: mvn -B -o -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am clean test -Djacoco.skip=true"]
exit-code: 0
tests-total: 249
tests-passed: 249
tests-failed: 0
environment: "maven-3.9.12-temurin-17.0.18-network-none"
dedupe-key: "issue-128-java17-offline-247444e"
---

# Evidence evidence-000002

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
