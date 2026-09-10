---
schema: "agora/tool/v1"
id: "llm-enrichment"
name: "Governed Renovatio LLM enrichment"
version: "1.0.0"
dependencies: []
category: "ai-enrichment"
executable: "renovatio-llm/bin/renovatio-llm-enrich"
authentication-reference: "environment-only-provider-credentials"
timeout-seconds: 180
max-output-bytes: 1048576
---

# Governed Renovatio LLM enrichment

Wraps one cache-miss lifecycle. Inputs contain reviewed identifiers and hashes only. Provider
credentials, raw prompts, unrestricted IR, authorization headers, and raw provider envelopes are
forbidden from tool inputs and results.
