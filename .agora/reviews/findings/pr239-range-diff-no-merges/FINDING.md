---
schema: "agora/review-finding/v1"
id: "pr239-range-diff-no-merges"
swarm: "architecture-convergence-2026"
work: "baseline-reconciliation"
pass: "codex-pr239-r5"
severity: "medium"
status: "open"
policy: "audit.range-diff-semantics"
location: "scripts/audit-branch-convergence.sh:36"
created-at: "2026-09-09T00:05:26.734737Z"
decided-by: null
decided-at: null
decision-reason: null
---

# Review finding pr239-range-diff-no-merges

## Summary

Use non-merge commit counts in synthesized ancestor and descendant range-diff summaries and correct the #217 report value.
