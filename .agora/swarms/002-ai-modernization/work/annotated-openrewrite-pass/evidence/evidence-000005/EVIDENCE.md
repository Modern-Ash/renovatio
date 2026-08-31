---
schema: "agora/evidence-entry/v3"
id: "evidence-000005"
type: "unit-tests"
phase: "verification"
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/annotated-openrewrite-pass-test-report-20260831-final.md"]
artifact-content-sha256: {"repo://docs/reports/annotated-openrewrite-pass-test-report-20260831-final.md":"b337c688d1e172b484a35ee93664b59b79a2c1cd96746140750a414fa3d13574"}
produced-by: "project:agent"
timestamp: "2026-08-31T12:17:06.828470Z"
tested-commit: "2345b6506a316a5bee2d0af0176208b77f3a99be"
command: ["mvn","-q","-pl","renovatio-cobol-annotations,cobol-openrewrite-recipes,renovatio-cobol-ir,renovatio-cobol-runtime,renovatio-provider-cobol","clean","test","-o","-Djacoco.skip=true"]
exit-code: 0
tests-total: 163
tests-passed: 163
tests-failed: 0
environment: "host-java21-clean-copy"
dedupe-key: "issue-127-unit-tests-final-2345b65"
---

# Evidence evidence-000005

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
