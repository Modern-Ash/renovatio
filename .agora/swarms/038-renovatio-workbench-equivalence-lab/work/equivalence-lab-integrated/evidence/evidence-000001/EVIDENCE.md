---
schema: "agora/evidence-entry/v3"
id: "evidence-000001"
type: "test"
phase: "verification"
result: "success"
revision: 1
artifact-references: [".agora/swarms/038-renovatio-workbench-equivalence-lab/work/equivalence-lab-integrated/VERIFICATION.md"]
artifact-content-sha256: {".agora/swarms/038-renovatio-workbench-equivalence-lab/work/equivalence-lab-integrated/VERIFICATION.md":null}
produced-by: "project:agent"
timestamp: "2026-09-07T20:44:17.970251Z"
tested-commit: null
command: ["mvn -q -pl renovatio-api -am -Dtest=WorkbenchEquivalenceLabApiTest,WorkbenchEquivalenceServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -Dexec.skip=true"]
exit-code: 0
tests-total: null
tests-passed: null
tests-failed: null
environment: "local"
dedupe-key: null
---

# Evidence evidence-000001

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
