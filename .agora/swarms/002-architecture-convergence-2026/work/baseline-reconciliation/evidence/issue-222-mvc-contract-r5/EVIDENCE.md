---
schema: "agora/evidence-entry/v3"
id: "issue-222-mvc-contract-r5"
type: "test"
phase: "review-revalidation"
result: "success"
revision: 5
artifact-references: ["repo://docs/reports/issue-222-baseline-test-report.md"]
artifact-content-sha256: {"repo://docs/reports/issue-222-baseline-test-report.md":"5308e02da53ca7820bdac22eb6fe16115057a577d6ce90577e1004af9d9a3bf6"}
produced-by: "project:agent"
timestamp: "2026-09-09T00:13:48.204810Z"
tested-commit: "b765b20e2440aeff99cba1010c239fb2c48dc841"
command: ["mvn","-o","-pl","renovatio-provider-cobol","-am","-Dtest=ArchitectureTransformerTest#supportsLayeredProfilesAndRejectsDuplicateProfiles,JavaArchitectureLayoutPlannerTest#plansConditionalCicsControllerInBothLayouts,JavaGenerationRegistryRoutingTest#cicsControllerIsPlannedBeforeManifestValidation","-Dsurefire.failIfNoSpecifiedTests=false","-Dexec.skip=true","-Djacoco.skip=true","test"]
exit-code: 0
tests-total: 3
tests-passed: 3
tests-failed: 0
environment: "local-jdk21-maven3.9.12"
dedupe-key: null
---

# Evidence issue-222-mvc-contract-r5

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
