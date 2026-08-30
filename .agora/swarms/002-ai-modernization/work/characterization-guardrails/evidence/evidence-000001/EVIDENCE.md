---
schema: "agora/evidence-entry/v3"
id: "evidence-000001"
type: "test-report"
phase: "implementation"
result: "success"
revision: 1
artifact-references: ["repo://docs/test-plans/characterization-guardrails.md"]
artifact-content-sha256: {"repo://docs/test-plans/characterization-guardrails.md":"4e56b0ac475615a40609109593d75c69e8ae1c1e70dc80be72514172aaf2b925"}
produced-by: "project:agent"
timestamp: "2026-08-30T16:05:43.381101Z"
tested-commit: null
command: ["mvn -B -pl renovatio-provider-cobol -Dtest=GuardrailSchemaCatalogTest,ManualActionItemWriterTest,ManualActionItemIdsTest -Dsurefire.failIfNoSpecifiedTests=false test"]
exit-code: 0
tests-total: 5
tests-passed: 5
tests-failed: 0
environment: "local-java17"
dedupe-key: "issue-122-guardrail-foundation-tests"
---

# Evidence evidence-000001

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
