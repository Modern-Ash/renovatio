---
schema: "agora/review-finding/v1"
id: "pr157-decision-patch-lost-update"
swarm: "decision-engine-f1"
work: "f1-decision-layer"
pass: "codex-pr-157"
severity: "high"
status: "open"
policy: "atomic-revision-conflict"
location: "renovatio-api/src/main/java/org/shark/renovatio/api/service/DecisionLayerService.java:49"
created-at: "2026-09-01T12:00:05.813154Z"
decided-by: null
decided-at: null
decision-reason: null
---

# Review finding pr157-decision-patch-lost-update

## Summary

Concurrent decision PATCH requests can both accept the same stale revision and silently overwrite one choice
