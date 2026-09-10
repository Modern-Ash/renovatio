---
schema: "agora/review-finding/v1"
id: "pr135-anthropic-response-bound"
swarm: "ai-modernization"
work: "llm-runtime-catalog-cache"
pass: "codex-pr135"
severity: "medium"
status: "resolved"
policy: "resource-bounds"
location: "renovatio-llm/src/main/java/org/shark/renovatio/llm/provider/AnthropicHttpTransport.java:41"
created-at: "2026-08-30T23:05:37.613315Z"
decided-by: "project:agent"
decided-at: "2026-08-30T23:09:57.801740Z"
decision-reason: "Anthropic transport now consumes BodyHandlers.ofInputStream and reads at most MAX_RESPONSE_BYTES + 1 before UTF-8 materialization; boundary and oversize regressions pass."
---

# Review finding pr135-anthropic-response-bound

## Summary

Anthropic BodyHandlers.ofString buffers the full response before the 1 MiB validation, so the advertised memory bound is not enforced during transport.
