---
schema: "agora/review-finding/v1"
id: "pr157-decision-patch-lost-update"
swarm: "decision-engine-f1"
work: "f1-decision-layer"
pass: "codex-pr-157"
severity: "high"
status: "resolved"
policy: "atomic-revision-conflict"
location: "renovatio-api/src/main/java/org/shark/renovatio/api/service/DecisionLayerService.java:49"
created-at: "2026-09-01T12:00:05.813154Z"
decided-by: "project:agent"
decided-at: "2026-09-01T12:05:39.853815Z"
decision-reason: "Commit 6929212 makes PATCH transactional, flushes the managed version, maps optimistic conflicts to 409, and proves one 200 plus one 409 for concurrent requests using the same revision"
---

# Review finding pr157-decision-patch-lost-update

## Summary

Concurrent decision PATCH requests can both accept the same stale revision and silently overwrite one choice
