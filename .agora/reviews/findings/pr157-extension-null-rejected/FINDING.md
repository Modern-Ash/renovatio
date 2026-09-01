---
schema: "agora/review-finding/v1"
id: "pr157-extension-null-rejected"
swarm: "decision-engine-f1"
work: "f1-decision-layer"
pass: "codex-pr-157"
severity: "medium"
status: "resolved"
policy: "profile-extension-contract"
location: "renovatio-profile/src/main/java/org/shark/renovatio/profile/MigrationProfile.java:21"
created-at: "2026-09-01T12:00:06.074142Z"
decided-by: "project:agent"
decided-at: "2026-09-01T12:05:40.134701Z"
decision-reason: "Commit 6929212 uses a null-tolerant unmodifiable defensive copy with JSON, YAML, and persisted API round-trip coverage"
---

# Review finding pr157-extension-null-rejected

## Summary

MigrationProfile rejects null values inside the open extensions namespace during defensive copying
