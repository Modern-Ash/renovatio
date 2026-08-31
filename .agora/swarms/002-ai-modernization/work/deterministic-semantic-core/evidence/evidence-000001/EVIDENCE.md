---
schema: "agora/evidence-entry/v3"
id: "evidence-000001"
type: "unit-tests"
phase: "implementation-revalidation"
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/deterministic-semantic-core-revalidation-20260831.md"]
artifact-content-sha256: {"repo://docs/reports/deterministic-semantic-core-revalidation-20260831.md":"981214902d2c42004f2bb0751250567a437879aded29c96fa93386cff26098a5"}
produced-by: "project:agent"
timestamp: "2026-08-31T01:12:37.660039Z"
tested-commit: "03e6b0dd0f069ec6a8c994ccd75f5253de2c6f64"
command: ["env","JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64","mvn","-B","clean","-pl","renovatio-cobol-runtime,renovatio-cobol-ir,cobol-openrewrite-recipes,renovatio-provider-cobol","-am","test"]
exit-code: 0
tests-total: 196
tests-passed: 196
tests-failed: 0
environment: "local-java17"
dedupe-key: "issue-123-java17-revalidation-03e6b0d"
---

# Evidence evidence-000001

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
