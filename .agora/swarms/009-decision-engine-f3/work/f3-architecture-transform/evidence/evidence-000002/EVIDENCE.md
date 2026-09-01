---
schema: "agora/evidence-entry/v3"
id: "evidence-000002"
type: "characterization"
phase: "verifying"
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/f3-architecture-transform-verification.md"]
artifact-content-sha256: {"repo://docs/reports/f3-architecture-transform-verification.md":"7788b45ad54dba4b98dc056840fa1a4cc477466b638c07af3caf2c0818c93e2c"}
produced-by: "project:agent"
timestamp: "2026-09-01T20:24:44.181408Z"
tested-commit: "9056f104b0f60906be9f59c8ead42790770d14a7"
command: ["mvn -pl renovatio-provider-cobol -am -Dexec.skip=true -Dtest=CharacterizationFixtureContractTest -Dsurefire.failIfNoSpecifiedTests=false test"]
exit-code: 0
tests-total: 2
tests-passed: 2
tests-failed: 0
environment: "local-jdk21"
dedupe-key: "f3-characterization-9056f104"
---

# Evidence evidence-000002

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
