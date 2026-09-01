---
schema: "agora/evidence-entry/v3"
id: "evidence-000003"
type: "review-remediation"
phase: null
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/f2-semantic-ir-emitter-spi-verification.md"]
artifact-content-sha256: {"repo://docs/reports/f2-semantic-ir-emitter-spi-verification.md":"ba9c98d8f098caedfefcefe414ac8d14299aeb1f38ce2fee0c6b081064f08c59"}
produced-by: "project:agent"
timestamp: "2026-09-01T14:57:55.762992Z"
tested-commit: "2d05633a3ce6bc372ef2b3928371e6201c2718d5"
command: ["mvn -pl renovatio-provider-cobol -am test; mvn -pl renovatio-api -am -Dexec.skip=true -Dtest=DecisionLayerApiTest -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl renovatio-mcp-server,renovatio-cli -am -Dexec.skip=true test"]
exit-code: 0
tests-total: 348
tests-passed: 348
tests-failed: 0
environment: "Maven 3.9.12; OpenJDK 21.0.12; source release 17"
dedupe-key: "f2-pr159-review-2d05633a"
---

# Evidence evidence-000003

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
