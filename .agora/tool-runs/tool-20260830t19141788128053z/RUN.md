---
schema: "agora/tool-run/v1"
id: "tool-20260830t19141788128053z"
tool: "llm-enrichment"
operation: "enrich"
actor: "project:agent"
swarm: "ai-modernization"
work: "llm-runtime-catalog-cache"
environment: null
capability: "llm.enrichment.execute"
risk: "write"
inputs: {"prompt-id":"cobol.domain.naming.v1","provider":"offline-fake","model":"fixture-v1","input-hash":"91d438f6ae0d184c2af8854723c580317e878d7905fcbfb4f18c44e9d75a4c47","cache-key":"f13b4bf91f60efadf4c87977b54d162cb5a370384ec2d9f86c93cf43e3eeffc5","schema-hash":"c3c0d42ae78fb3dea7b3d20ccc622f37d22caa6f5206cf958b095c7d2f1f7a54","runtime-contract-version":"renovatio-llm.v1"}
command: ["renovatio-llm/bin/renovatio-llm-enrich","enrich","--prompt-id","cobol.domain.naming.v1","--provider","offline-fake","--model","fixture-v1","--input-hash","91d438f6ae0d184c2af8854723c580317e878d7905fcbfb4f18c44e9d75a4c47","--cache-key","f13b4bf91f60efadf4c87977b54d162cb5a370384ec2d9f86c93cf43e3eeffc5","--schema-hash","c3c0d42ae78fb3dea7b3d20ccc622f37d22caa6f5206cf958b095c7d2f1f7a54","--runtime-contract-version","renovatio-llm.v1"]
runtime-available: true
status: "completed"
result-kind: "llm-enrichment-attribution"
timeout-seconds: 180
max-output-bytes: 1048576
authentication-reference: "environment-only-provider-credentials"
created-at: "2026-08-30T19:14:13.920204Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260830t19141788128053z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
