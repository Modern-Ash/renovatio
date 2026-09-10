---
schema: "agora/evidence-entry/v3"
id: "evidence-000003"
type: "test-report"
phase: "review-fix"
result: "success"
revision: 1
artifact-references: ["repo://docs/test-plans/characterization-guardrails.md"]
artifact-content-sha256: {"repo://docs/test-plans/characterization-guardrails.md":"4e56b0ac475615a40609109593d75c69e8ae1c1e70dc80be72514172aaf2b925"}
produced-by: "project:agent"
timestamp: "2026-08-30T16:25:06.057924Z"
tested-commit: null
command: ["mvn -B -pl renovatio-provider-cobol -Dtest=GuardrailGateRunnerTest,GuardrailSchemaCatalogTest,ManualActionItemWriterTest,ManualActionItemIdsTest,ManualActionItemTypeTest -Dsurefire.failIfNoSpecifiedTests=false test"]
exit-code: 0
tests-total: 11
tests-passed: 11
tests-failed: 0
environment: "local-java17"
dedupe-key: "pr-129-review-fixes"
---

# Evidence evidence-000003

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
