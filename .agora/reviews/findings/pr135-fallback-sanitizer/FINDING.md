---
schema: "agora/review-finding/v1"
id: "pr135-fallback-sanitizer"
swarm: "ai-modernization"
work: "llm-runtime-catalog-cache"
pass: "codex-pr135"
severity: "high"
status: "resolved"
policy: "deterministic-fallback"
location: "renovatio-llm/src/main/java/org/shark/renovatio/llm/enrichment/PersistenceSanitizer.java:47"
created-at: "2026-08-30T23:05:26.993199Z"
decided-by: "project:agent"
decided-at: "2026-08-30T23:09:57.486774Z"
decision-reason: "PersistenceSanitizer now validates deterministicResult under an open deterministic JSON contract while retaining forbidden-field, secret, size and JSON-type checks; regression tests cover valid domain fields and unsafe content."
---

# Review finding pr135-fallback-sanitizer

## Summary

Fallback deterministicResult is recursively checked against the LLM output field allowlist, so valid deterministic fields can abort fallback attribution.
