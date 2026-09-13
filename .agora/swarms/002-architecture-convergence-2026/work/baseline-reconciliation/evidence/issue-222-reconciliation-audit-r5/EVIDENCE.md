---
schema: "agora/evidence-entry/v3"
id: "issue-222-reconciliation-audit-r5"
type: "audit"
phase: "review-revalidation"
result: "success"
revision: 5
artifact-references: ["repo://docs/reports/issue-222-branch-reconciliation.md","repo://scripts/audit-branch-convergence.sh"]
artifact-content-sha256: {"repo://docs/reports/issue-222-branch-reconciliation.md":"70f7f2db51f094a113fd64e7965c936aa232cfd002b1bc97a080c8b91826fcab","repo://scripts/audit-branch-convergence.sh":"64901cf9e7b56d5f5596bda037ac1dcefffda95ca10548b2eaf4045229cd1b47"}
produced-by: "project:agent"
timestamp: "2026-09-09T00:13:48.764356Z"
tested-commit: "a7a7a9dcbd23370a476ae71a8482448699850f83"
command: ["./scripts/audit-branch-convergence.sh","origin/main","refs/remotes/workspace/agora/decision-engine-f8","refs/remotes/workspace/agora/renovatio-workbench-bootstrap","refs/remotes/workspace/agora/issue-217-carddemo-coverage"]
exit-code: 0
tests-total: null
tests-passed: null
tests-failed: null
environment: "local-git"
dedupe-key: null
---

# Evidence issue-222-reconciliation-audit-r5

This append-only record captures a governed verification fact. Provider output and credentials are intentionally excluded.
