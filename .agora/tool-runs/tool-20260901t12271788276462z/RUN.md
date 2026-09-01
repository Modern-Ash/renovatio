---
schema: "agora/tool-run/v1"
id: "tool-20260901t12271788276462z"
tool: "repository"
operation: "create-branch"
actor: "project:agent"
swarm: "decision-engine-f2"
work: "f2-semantic-ir-emitter-spi"
environment: null
capability: "repository.write"
risk: "write"
inputs: {"branch":"agora/f2-semantic-ir-emitter"}
command: ["git","checkout","-b","agora/f2-semantic-ir-emitter"]
runtime-available: true
status: "failed"
result-kind: "repository-change"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "local-git-configuration"
created-at: "2026-09-01T12:27:42.081752Z"
exit-code: 128
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260901t12271788276462z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
