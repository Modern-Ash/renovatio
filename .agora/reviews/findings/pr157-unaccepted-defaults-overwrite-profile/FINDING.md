---
schema: "agora/review-finding/v1"
id: "pr157-unaccepted-defaults-overwrite-profile"
swarm: "decision-engine-f1"
work: "f1-decision-layer"
pass: "codex-pr-157"
severity: "high"
status: "resolved"
policy: "profile-precedence"
location: "renovatio-profile/src/main/java/org/shark/renovatio/profile/MigrationProfiles.java:179"
created-at: "2026-09-01T12:00:05.548664Z"
decided-by: "project:agent"
decided-at: "2026-09-01T12:05:39.575694Z"
decision-reason: "Commit 6929212 separates accepted profile overrides from resolved defaults; regression coverage preserves FLUENT and NONE while AUTO decisions remain unapplied"
---

# Review finding pr157-unaccepted-defaults-overwrite-profile

## Summary

Decision catalog defaults overwrite explicit profile naming and framework before those decisions are confirmed or overridden
