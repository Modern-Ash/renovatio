---
schema: "agora/tool-run/v1"
id: "tool-20260830t16581788119920z"
tool: "repository"
operation: "create-branch"
actor: "project:agent"
swarm: "ai-modernization"
work: "annotated-ir-contract"
environment: null
capability: "repository.write"
risk: "write"
inputs: {"branch":"agora/issue-124-annotated-ir-contract"}
command: ["git","checkout","-b","agora/issue-124-annotated-ir-contract"]
runtime-available: true
status: "completed"
result-kind: "repository-change"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "local-git-configuration"
created-at: "2026-08-30T16:58:40.230141Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260830t16581788119920z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
