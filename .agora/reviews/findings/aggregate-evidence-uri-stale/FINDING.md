---
schema: "agora/review-finding/v1"
id: "aggregate-evidence-uri-stale"
swarm: "ai-modernization"
work: "residual-semantic-enrichment"
pass: "pr-136-review"
severity: "high"
status: "resolved"
policy: "evidence-integrity-v1"
location: ".agora/swarms/002-ai-modernization/work/residual-semantic-enrichment/evidence.md"
created-at: "2026-08-31T00:54:01.314874Z"
decided-by: "project:agent"
decided-at: "2026-08-31T01:03:20.476705Z"
decision-reason: "Revision 2 records only the immutable PR #136 report URI in the current aggregate evidence register; revision 1 remains an immutable historical snapshot and agora validate is ok."
---

# Review finding aggregate-evidence-uri-stale

## Summary

Aggregate evidence rows for rework still reference the mutable report and stale digest instead of the versioned authoritative revalidation report.
