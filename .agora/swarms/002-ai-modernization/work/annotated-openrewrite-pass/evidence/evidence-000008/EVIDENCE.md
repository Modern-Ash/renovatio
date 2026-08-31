---
schema: "agora/evidence-entry/v3"
id: "evidence-000008"
type: "offline-characterization"
phase: "review-fix-verification"
result: "success"
revision: 2
artifact-references: ["repo://docs/reports/annotated-openrewrite-pass-pr139-review-20260831.md"]
artifact-content-sha256: {"repo://docs/reports/annotated-openrewrite-pass-pr139-review-20260831.md":"28071d9be150161ff36b6da424eea54b861fdf95a74594c4cde828985d76fecc"}
produced-by: "project:agent"
timestamp: "2026-08-31T12:50:16.068559Z"
tested-commit: "294e0917857a6e29a085f684b03750f0d7a2e31f"
command: ["mvn","-B","-o","-pl","renovatio-provider-cobol,cobol-openrewrite-recipes","-am","clean","test","-Djacoco.skip=true"]
exit-code: 0
tests-total: 63
tests-passed: 63
tests-failed: 0
environment: "docker-temurin17-network-none"
dedupe-key: "pr139-review-offline-ci-294e091"
---

# Evidence evidence-000008

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
