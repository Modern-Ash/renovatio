---
schema: "agora/review-finding/v1"
id: "pr135-promotion-history"
swarm: "ai-modernization"
work: "llm-runtime-catalog-cache"
pass: "codex-pr135"
severity: "low"
status: "waived"
policy: "cache-promotion-history"
location: "renovatio-llm/src/main/java/org/shark/renovatio/llm/cache/GovernedPromotionVerifier.java:22"
created-at: "2026-08-30T23:05:37.910801Z"
decided-by: "project:owner"
decided-at: "2026-08-30T23:05:38.210266Z"
decision-reason: "Not reproducible at reviewed commit dd336681: git log identifies manifest-only commit d547e8f; A f94af741, B 4f242190, C 488d62db and D d547e8f are all ancestors of HEAD."
---

# Review finding pr135-promotion-history

## Summary

Review alleged that the manifest was introduced in a 222-file commit with unreachable A/B/C history.
