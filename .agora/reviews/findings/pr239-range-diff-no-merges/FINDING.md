---
schema: "agora/review-finding/v1"
id: "pr239-range-diff-no-merges"
swarm: "architecture-convergence-2026"
work: "baseline-reconciliation"
pass: "codex-pr239-r5"
severity: "medium"
status: "resolved"
policy: "audit.range-diff-semantics"
location: "scripts/audit-branch-convergence.sh:36"
created-at: "2026-09-09T00:05:26.734737Z"
decided-by: "project:agent"
decided-at: "2026-09-09T00:15:44.123721Z"
decision-reason: "Both synthesized ancestor and descendant summaries now use git rev-list --no-merges --count. Revalidation produced 0/0/11/0 and 0/11/0/0 respectively; the current report documents raw 0/16 separately, and historical report versions were archived byte-for-byte."
---

# Review finding pr239-range-diff-no-merges

## Summary

Use non-merge commit counts in synthesized ancestor and descendant range-diff summaries and correct the #217 report value.
