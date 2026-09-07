---
schema: "agora/evidence-entry/v3"
id: "evidence-000006"
type: "review-remediation"
phase: "fourth-review"
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/f2-semantic-ir-emitter-spi-verification.md"]
artifact-content-sha256: {"repo://docs/reports/f2-semantic-ir-emitter-spi-verification.md":"d0475edc67aa571694c96e4ff3b9946ebc62e2f1cb447029d6b68fe66a2a7b86"}
produced-by: "project:agent"
timestamp: "2026-09-01T17:22:11.794451Z"
tested-commit: "eaf429edbca5ee90e9c6a145aeedafda2748231c"
command: ["mvn -pl renovatio-provider-cobol -am test -q","mvn -pl renovatio-api -Dtest=DecisionLayerApiTest -Dexec.skip=true test -q","mvn -pl renovatio-mcp-server test -q","mvn -pl renovatio-cli test -q"]
exit-code: 0
tests-total: 354
tests-passed: 354
tests-failed: 0
environment: null
dedupe-key: "pr-159-fourth-review-eaf429ed"
---

# Evidence evidence-000006

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
