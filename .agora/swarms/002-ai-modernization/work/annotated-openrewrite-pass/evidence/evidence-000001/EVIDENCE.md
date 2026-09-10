---
schema: "agora/evidence-entry/v3"
id: "evidence-000001"
type: "unit-tests"
phase: "verification"
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/annotated-openrewrite-pass-test-report-20260831.md"]
artifact-content-sha256: {"repo://docs/reports/annotated-openrewrite-pass-test-report-20260831.md":"abf76351e3bc84c91241426a3f3d68910d92fd87ca9628bd39db3ab99023192d"}
produced-by: "project:agent"
timestamp: "2026-08-31T12:07:09.445911Z"
tested-commit: "25a586fb0cba3fa73726e05d54435b99eb1628dc"
command: ["mvn -q -pl renovatio-cobol-annotations,cobol-openrewrite-recipes,renovatio-cobol-ir,renovatio-cobol-runtime,renovatio-provider-cobol test -o -Djacoco.skip=true"]
exit-code: 0
tests-total: 160
tests-passed: 160
tests-failed: 0
environment: null
dedupe-key: "issue-127-unit-tests-25a586f"
---

# Evidence evidence-000001

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
