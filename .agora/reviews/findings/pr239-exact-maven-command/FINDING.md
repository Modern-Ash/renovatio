---
schema: "agora/review-finding/v1"
id: "pr239-exact-maven-command"
swarm: "architecture-convergence-2026"
work: "baseline-reconciliation"
pass: "codex-pr-239"
severity: "medium"
status: "resolved"
policy: "evidence.reproducibility"
location: ".agora/swarms/048-architecture-convergence-2026/work/baseline-reconciliation/evidence/issue-222-mvc-contract/EVIDENCE.md:13"
created-at: "2026-09-08T23:24:19.396692Z"
decided-by: "project:agent"
decided-at: "2026-09-08T23:28:02.819296Z"
decision-reason: "Replaced the non-goal placeholder with the exact -Dtest selector, reran that exact command successfully (3/3 tests), and registered revision-2 reproducible evidence."
---

# Review finding pr239-exact-maven-command

## Summary

MVC/CICS evidence records targeted-mvc-cics-triad instead of the exact -Dtest selector, so the command is not replayable.
