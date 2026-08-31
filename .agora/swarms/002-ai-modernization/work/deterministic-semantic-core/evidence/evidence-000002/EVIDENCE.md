---
schema: "agora/evidence-entry/v3"
id: "evidence-000002"
type: "unit-tests"
phase: "implementation"
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/deterministic-semantic-core-recipe-boundary-20260831.md"]
artifact-content-sha256: {"repo://docs/reports/deterministic-semantic-core-recipe-boundary-20260831.md":"b3145316c2fff60c20e9ce2a0ad46375c64c1108508124ed038a69e237f26561"}
produced-by: "project:agent"
timestamp: "2026-08-31T01:18:53.487604Z"
tested-commit: null
command: ["JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64","mvn","-B","-pl","cobol-openrewrite-recipes","-am","-Dtest=PopulateCobolProcessRecipeTest","-Dsurefire.failIfNoSpecifiedTests=false","test"]
exit-code: 0
tests-total: 4
tests-passed: 4
tests-failed: 0
environment: "java-17-local"
dedupe-key: "deterministic-semantic-core-recipe-boundary-20260831"
---

# Evidence evidence-000002

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
