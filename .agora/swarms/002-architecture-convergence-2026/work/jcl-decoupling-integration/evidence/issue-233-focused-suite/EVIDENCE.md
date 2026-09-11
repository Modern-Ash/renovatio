---
schema: "agora/evidence-entry/v3"
id: "issue-233-focused-suite"
type: "test"
phase: "verification"
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/issue-233-test-report.md"]
artifact-content-sha256: {"repo://docs/reports/issue-233-test-report.md":"36b75b566cbc4d010ca060a783315fb52434b515ed3661627766904468aa0bea"}
produced-by: "project:agent"
timestamp: "2026-09-11T17:19:51.050757Z"
tested-commit: "9b5f1ede087630ea90a892255a004999be650b35"
command: ["mvn -q -pl renovatio-decisions,renovatio-llm,renovatio-jcl,renovatio-application -am -Dexec.skip=true -Dtest=DecisionSuggestionServiceTest,BatchDecisionPointsTest,F7AcceptanceTest,ReviewRegressionTest,DefaultRenovatioApplicationTest,ApplicationArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test"]
exit-code: 0
tests-total: null
tests-passed: null
tests-failed: null
environment: "local-java"
dedupe-key: null
---

# Evidence issue-233-focused-suite

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
