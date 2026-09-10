---
schema: "agora/evidence-entry/v3"
id: "evidence-000006"
type: "coverage"
phase: "verification"
result: "success"
revision: 2
artifact-references: ["repo://docs/reports/carddemo-coverage.md"]
artifact-content-sha256: {"repo://docs/reports/carddemo-coverage.md":"1f62017f488a34c95728e2df9d0720132a2838522a7d284125f5a40a05c25e7c"}
produced-by: "project:agent"
timestamp: "2026-09-08T22:13:46.242851Z"
tested-commit: "b74a8cb3ff02db40de5138841943c26d5678bc48"
command: ["mvn -o -pl renovatio-provider-cobol test -Dgroups=coverage -Drenovatio.surefire.excludedGroups= -Dexec.skip=true -Djacoco.skip=true"]
exit-code: 0
tests-total: 1
tests-passed: 1
tests-failed: 0
environment: "local-offline"
dedupe-key: "issue-207-revalidation-carddemo-b74a8cb3"
---

# Evidence evidence-000006

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
