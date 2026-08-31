---
schema: "agora/review-finding/v1"
id: "domain-request-governance"
swarm: "ai-modernization"
work: "residual-semantic-enrichment"
pass: "contract-review"
severity: "high"
status: "resolved"
policy: "domain-naming-request-v1"
location: "renovatio-llm/src/main/java/org/shark/renovatio/llm/residual/ResidualEnrichmentRequest.java"
created-at: "2026-08-31T00:34:27.313183Z"
decided-by: "project:agent"
decided-at: "2026-08-31T00:35:25.732850Z"
decision-reason: "Commit 8c3e0f0 adds collision scope, public-signature disposition, and validated Agora tool-run identity to the request contract."
---

# Review finding domain-request-governance

## Summary

Domain naming request omits collision scope, public-signature disposition, and Agora tool-run identity required by the authoritative spec.
