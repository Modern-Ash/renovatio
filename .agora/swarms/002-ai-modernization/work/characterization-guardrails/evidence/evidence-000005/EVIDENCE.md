---
schema: "agora/evidence-entry/v3"
id: "evidence-000005"
type: "offline-ci"
phase: "implementation"
result: "success"
revision: 1
artifact-references: ["repo://docs/test-reports/characterization-guardrails-offline.md"]
artifact-content-sha256: {"repo://docs/test-reports/characterization-guardrails-offline.md":"9eb8ed465f1b0f9dc8e5e1a26615cdd4058792c38537ea67d4b3e742d6c927fe"}
produced-by: "project:agent"
timestamp: "2026-08-31T01:47:09.433955Z"
tested-commit: "8ef20188feccbccf9c50796e1a55773fab8f8b63"
command: ["docker","run","--rm","--network=none","mvn","-B","-o","-pl","renovatio-provider-cobol,cobol-openrewrite-recipes","-am","test"]
exit-code: 0
tests-total: 201
tests-passed: 201
tests-failed: 0
environment: "docker-java17-offline"
dedupe-key: "issue122-offline-ci-v1"
---

# Evidence evidence-000005

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
