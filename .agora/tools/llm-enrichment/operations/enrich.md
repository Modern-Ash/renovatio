---
schema: "agora/tool-operation/v1"
id: "enrich"
name: "Enrich one canonical annotated-IR input"
capability: "llm.enrichment.execute"
risk: "write"
arguments: ["enrich","--prompt-id","{prompt-id}","--provider","{provider}","--model","{model}","--input-hash","{input-hash}","--cache-key","{cache-key}","--schema-hash","{schema-hash}","--runtime-contract-version","{runtime-contract-version}"]
inputs: ["prompt-id","provider","model","input-hash","cache-key","schema-hash","runtime-contract-version"]
result-kind: "llm-enrichment-attribution"
---

# Enrich one canonical input

The operation starts before any provider call and finishes only after a sanitized pending cache
candidate or deterministic fallback exists. Finalization failure quarantines the candidate.
