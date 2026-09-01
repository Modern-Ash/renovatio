---
schema: "agora/evidence-entry/v3"
id: "evidence-000004"
type: "review-remediation"
phase: "re-review"
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/f2-semantic-ir-emitter-spi-verification.md"]
artifact-content-sha256: {"repo://docs/reports/f2-semantic-ir-emitter-spi-verification.md":"e41c0f2e4ff1c4175d3d4f6f5fcc845d202b28466667535a5a645bd90d488fd6"}
produced-by: "project:agent"
timestamp: "2026-09-01T15:18:33.676231Z"
tested-commit: "484bf70757c8b24ca7a5b2ddf5d2fb9e545a4341"
command: ["mvn -pl renovatio-provider-cobol -am test -q","mvn -pl renovatio-api -Dtest=DecisionLayerApiTest -Dexec.skip=true test -q","mvn -pl renovatio-mcp-server test -q","mvn -pl renovatio-cli test -q"]
exit-code: 0
tests-total: 352
tests-passed: 352
tests-failed: 0
environment: null
dedupe-key: "pr-159-rereview-484bf707"
---

# Evidence evidence-000004

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
