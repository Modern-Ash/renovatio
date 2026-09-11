---
schema: "agora/evidence-entry/v3"
id: "evidence-000001"
type: "test"
phase: "post-merge-review"
result: "success"
revision: 1
artifact-references: [".agora/swarms/051-issue-228-cobol-java-reference-path/work/cobol-java-reference-path/test-report.md"]
artifact-content-sha256: {".agora/swarms/051-issue-228-cobol-java-reference-path/work/cobol-java-reference-path/test-report.md":null}
produced-by: "project:agent"
timestamp: "2026-09-11T13:36:43.989043Z"
tested-commit: "d3c5e788d9d72c248db5970670e14334babd9579"
command: ["./mvnw -q -pl renovatio-provider-cobol,renovatio-cli,renovatio-api -am -Dtest=PipelineE2ETest,PipelineValidationTest,SurfaceProofTest,FixturesExistenceTest,RenovatioCliSmokeTest,ReferencePipelineCommandTest,ReferencePipelineApiTest -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true -Dexec.skip=true test"]
exit-code: 0
tests-total: null
tests-passed: null
tests-failed: null
environment: "local"
dedupe-key: "issue-228-post-merge-pr252"
---

# Evidence evidence-000001

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
