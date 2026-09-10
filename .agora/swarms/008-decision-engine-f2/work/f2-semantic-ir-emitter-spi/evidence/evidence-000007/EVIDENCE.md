---
schema: "agora/evidence-entry/v3"
id: "evidence-000007"
type: "review-remediation"
phase: "fifth-review"
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/f2-semantic-ir-emitter-spi-verification.md"]
artifact-content-sha256: {"repo://docs/reports/f2-semantic-ir-emitter-spi-verification.md":"d0475edc67aa571694c96e4ff3b9946ebc62e2f1cb447029d6b68fe66a2a7b86"}
produced-by: "project:agent"
timestamp: "2026-09-01T17:35:28.564212Z"
tested-commit: "0f2b0f6c2c3d7abdc0a292dc3ae6f320239eeb6d"
command: ["mvn -pl renovatio-provider-cobol -am test -q","mvn -pl renovatio-api -Dtest=DecisionLayerApiTest -Dexec.skip=true test -q","mvn -pl renovatio-mcp-server test -q","mvn -pl renovatio-cli test -q"]
exit-code: 0
tests-total: 356
tests-passed: 356
tests-failed: 0
environment: null
dedupe-key: "f2-post-merge-review-0f2b0f6c"
---

# Evidence evidence-000007

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
