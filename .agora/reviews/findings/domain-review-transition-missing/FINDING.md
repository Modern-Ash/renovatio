---
schema: "agora/review-finding/v1"
id: "domain-review-transition-missing"
swarm: "ai-modernization"
work: "residual-semantic-enrichment"
pass: "pr-136-review"
severity: "high"
status: "open"
policy: "human-review-v1"
location: "renovatio-llm/src/main/java/org/shark/renovatio/llm/residual/HumanAnnotationReviewService.java"
created-at: "2026-08-31T00:54:01.020205Z"
decided-by: null
decided-at: null
decision-reason: null
---

# Review finding domain-review-transition-missing

## Summary

Domain naming annotations start PROPOSED but no production service can assign and decide them, so suggestions are not reviewable.
