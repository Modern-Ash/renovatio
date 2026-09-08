---
schema: "agora/review-finding/v1"
id: "pr239-revision2-ci-head"
swarm: "architecture-convergence-2026"
work: "baseline-reconciliation"
pass: "codex-pr-239-r2"
severity: "medium"
status: "resolved"
policy: "ci.commit-binding"
location: ".agora/swarms/048-architecture-convergence-2026/work/baseline-reconciliation/evidence/issue-222-pr-ci-r2/EVIDENCE.md"
created-at: "2026-09-08T23:36:23.139116Z"
decided-by: "project:agent"
decided-at: "2026-09-08T23:37:59.494786Z"
decision-reason: "Ran github-pull-requests/checks after caf23cb8 completed CI, registered Tool Run tool-20260908t23361788921393z, withdrew the predating evidence from the active set, and bound replacement evidence issue-222-pr-ci-caf23cb8 to the corrected commit."
---

# Review finding pr239-revision2-ci-head

## Summary

Revision-2 CI evidence references a Tool Run that predates the declared tested commit and does not validate caf23cb8.
