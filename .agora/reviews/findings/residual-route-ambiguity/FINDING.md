---
schema: "agora/review-finding/v1"
id: "residual-route-ambiguity"
swarm: "ai-modernization"
work: "residual-semantic-enrichment"
pass: "contract-review"
severity: "high"
status: "resolved"
policy: "residual-routing-v1"
location: "renovatio-llm/src/main/java/org/shark/renovatio/llm/residual/ResidualRouter.java"
created-at: "2026-08-31T00:34:27.075425Z"
decided-by: "project:agent"
decided-at: "2026-08-31T00:35:25.519493Z"
decision-reason: "Commit 8c3e0f0 adds fail-closed incompatible-signal detection and a zero-executor-call regression test."
---

# Review finding residual-route-ambiguity

## Summary

Router does not fail closed when incompatible residual signals coexist in one request.
