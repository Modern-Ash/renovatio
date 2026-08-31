---
schema: "agora/review-finding/v1"
id: "residual-route-ambiguity"
swarm: "ai-modernization"
work: "residual-semantic-enrichment"
pass: "contract-review"
severity: "high"
status: "open"
policy: "residual-routing-v1"
location: "renovatio-llm/src/main/java/org/shark/renovatio/llm/residual/ResidualRouter.java"
created-at: "2026-08-31T00:34:27.075425Z"
decided-by: null
decided-at: null
decision-reason: null
---

# Review finding residual-route-ambiguity

## Summary

Router does not fail closed when incompatible residual signals coexist in one request.
