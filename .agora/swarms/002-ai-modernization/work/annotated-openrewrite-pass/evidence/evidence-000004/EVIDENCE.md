---
schema: "agora/evidence-entry/v3"
id: "evidence-000004"
type: "offline-characterization"
phase: "verification"
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/annotated-openrewrite-pass-test-report-20260831.md"]
artifact-content-sha256: {"repo://docs/reports/annotated-openrewrite-pass-test-report-20260831.md":"abf76351e3bc84c91241426a3f3d68910d92fd87ca9628bd39db3ab99023192d"}
produced-by: "project:agent"
timestamp: "2026-08-31T12:15:06.954665Z"
tested-commit: "2345b6506a316a5bee2d0af0176208b77f3a99be"
command: ["mvn","-B","-o","-pl","renovatio-provider-cobol,cobol-openrewrite-recipes","-am","clean","test","-Djacoco.skip=true"]
exit-code: 0
tests-total: 62
tests-passed: 62
tests-failed: 0
environment: "docker-temurin17-network-none"
dedupe-key: "issue-127-offline-ci-2345b65"
---

# Evidence evidence-000004

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
