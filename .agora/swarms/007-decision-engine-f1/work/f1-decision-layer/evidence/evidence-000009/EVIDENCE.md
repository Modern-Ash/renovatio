---
schema: "agora/evidence-entry/v3"
id: "evidence-000009"
type: "review-fix-tests"
phase: "verification"
result: "success"
revision: 1
artifact-references: ["repo://docs/reports/f1-decision-layer-verification.md"]
artifact-content-sha256: {"repo://docs/reports/f1-decision-layer-verification.md":"6255e8649a3144d986f0c305b77818ef3e550653a93099e4563d34e4f937287c"}
produced-by: "project:agent"
timestamp: "2026-09-01T12:05:39.302034Z"
tested-commit: "6929212d5d04b97a2c16ebddc5b40aef171499b1"
command: ["mvn -o -pl renovatio-profile,renovatio-decisions,renovatio-api -am -Dtest=MigrationProfilesTest,DecisionDomainTest,DecisionLayerApiTest -Dsurefire.failIfNoSpecifiedTests=false test"]
exit-code: 0
tests-total: 24
tests-passed: 24
tests-failed: 0
environment: "local"
dedupe-key: "pr157-review-fixes"
---

# Evidence evidence-000009

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
