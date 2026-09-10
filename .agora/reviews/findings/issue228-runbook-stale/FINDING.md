---
schema: "agora/review-finding/v1"
id: "issue228-runbook-stale"
swarm: "issue-228-cobol-java-reference-path"
work: "cobol-java-reference-path"
pass: "post-merge-ac07-audit"
severity: "high"
status: "resolved"
policy: "runbook"
location: "docs/RUNBOOK.md"
created-at: "2026-09-10T12:59:58.183123Z"
decided-by: "project:agent"
decided-at: "2026-09-10T14:06:30.899684Z"
decision-reason: "Runbook now documents JDK 21, current stages, commands, artifact sets, and measured evidence."
---

# Review finding issue228-runbook-stale

## Summary

Runbook declares Java 17 and stale test totals even though the merged reactor requires Java 21; no clean-clone timing evidence is registered.
