---
schema: "agora/evidence-entry/v3"
id: "evidence-000005"
type: "test-suite"
phase: "verification"
result: "success"
revision: 2
artifact-references: ["repo://docs/reports/issue-207-perform-thru-varying-times-verification.md"]
artifact-content-sha256: {"repo://docs/reports/issue-207-perform-thru-varying-times-verification.md":"6e5ac1facbfbb91a3fc06947ce427dc251690dce7e0191c4b517ba70a523326c"}
produced-by: "project:agent"
timestamp: "2026-09-08T22:13:45.959692Z"
tested-commit: "b74a8cb3ff02db40de5138841943c26d5678bc48"
command: ["mvn -o -pl renovatio-cobol-ir,cobol-openrewrite-recipes,renovatio-provider-cobol test -Dexec.skip=true -Djacoco.skip=true"]
exit-code: 0
tests-total: 235
tests-passed: 235
tests-failed: 0
environment: "local-offline"
dedupe-key: "issue-207-revalidation-tests-b74a8cb3"
---

# Evidence evidence-000005

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
