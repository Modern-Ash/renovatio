---
schema: "agora/evidence-entry/v3"
id: "evidence-000002"
type: "test"
phase: "backend-workbench"
result: "success"
revision: 1
artifact-references: [".agora/swarms/037-renovatio-workbench-change-sets/work/change-sets-approval-rollback/verification.md"]
artifact-content-sha256: {".agora/swarms/037-renovatio-workbench-change-sets/work/change-sets-approval-rollback/verification.md":null}
produced-by: "project:agent"
timestamp: "2026-09-07T20:22:06.956225Z"
tested-commit: null
command: ["mvn -q -pl renovatio-api -am -Dtest=ArchitecturePreviewApiTest,WorkbenchChangeSetApiTest,WorkbenchDomainModelApiTest,DecisionLayerApiTest,WorkbenchArchitectureCanvasServiceTest,WorkbenchDomainModelServiceTest,WorkbenchSourceExplorerServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -Dexec.skip=true"]
exit-code: 0
tests-total: null
tests-passed: null
tests-failed: null
environment: "local"
dedupe-key: null
---

# Evidence evidence-000002

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
