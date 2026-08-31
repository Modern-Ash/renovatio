---
schema: "agora/review-finding/v1"
id: "control-flow-baseline-unbound"
swarm: "ai-modernization"
work: "residual-semantic-enrichment"
pass: "pr-136-review"
severity: "high"
status: "open"
policy: "characterization-baseline-v1"
location: "renovatio-llm/src/main/java/org/shark/renovatio/llm/residual/ControlFlowPlanGate.java"
created-at: "2026-08-31T00:54:00.727295Z"
decided-by: null
decided-at: null
decision-reason: null
---

# Review finding control-flow-baseline-unbound

## Summary

ControlFlowPlanGate accepts green evidence without proving it belongs to the baseline used to build the enrichment request.
