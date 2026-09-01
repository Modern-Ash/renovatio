---
schema: "agora/evidence-entry/v3"
id: "evidence-000005"
type: "review-remediation"
phase: "third-review"
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/f2-semantic-ir-emitter-spi-verification.md"]
artifact-content-sha256: {"repo://docs/reports/f2-semantic-ir-emitter-spi-verification.md":"f57abb3ba3f84f74d6c564b2b300f9e6c653f76971002eac741e76a24a7ed02a"}
produced-by: "project:agent"
timestamp: "2026-09-01T16:19:21.440036Z"
tested-commit: "6bee7cdb65ba18f1f85f4e18b04d8bfca28c3529"
command: ["mvn -pl renovatio-provider-cobol -am test -q","mvn -pl renovatio-api -Dtest=DecisionLayerApiTest -Dexec.skip=true test -q","mvn -pl renovatio-mcp-server test -q","mvn -pl renovatio-cli test -q"]
exit-code: 0
tests-total: 354
tests-passed: 354
tests-failed: 0
environment: null
dedupe-key: "pr-159-third-review-6bee7cdb"
---

# Evidence evidence-000005

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
