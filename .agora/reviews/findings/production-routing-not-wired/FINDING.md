---
schema: "agora/review-finding/v1"
id: "production-routing-not-wired"
swarm: "ai-modernization"
work: "residual-semantic-enrichment"
pass: "pr-136-review"
severity: "critical"
status: "open"
policy: "residual-routing-v1"
location: "renovatio-llm/src/main/java/org/shark/renovatio/llm/cli/LlmEnrichmentCli.java"
created-at: "2026-08-31T00:54:00.097616Z"
decided-by: null
decided-at: null
decision-reason: null
---

# Review finding production-routing-not-wired

## Summary

Production CLI bypasses ResidualEnrichmentCoordinator and accepts caller-supplied residual prompt IDs, allowing deterministic constructions to reach cache/provider.
