---
schema: "agora/tool-run/v1"
id: "tool-20260830t17021788120148z"
tool: "repository"
operation: "create-branch"
actor: "project:agent"
swarm: "ai-modernization"
work: "deterministic-semantic-core"
environment: null
capability: "repository.write"
risk: "write"
inputs: {"branch":"fix/issue-123-codex-review"}
command: ["git","checkout","-b","fix/issue-123-codex-review"]
runtime-available: true
status: "completed"
result-kind: "repository-change"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "local-git-configuration"
created-at: "2026-08-30T17:02:28.923580Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260830t17021788120148z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
