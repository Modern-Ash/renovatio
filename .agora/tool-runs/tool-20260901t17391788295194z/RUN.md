---
schema: "agora/tool-run/v1"
id: "tool-20260901t17391788295194z"
tool: "repository"
operation: "publish-branch"
actor: "project:agent"
swarm: "decision-engine-f2"
work: "f2-semantic-ir-emitter-spi"
environment: null
capability: "repository.write"
risk: "write"
inputs: {"branch":"agora/f2-final-review"}
command: ["git","push","--set-upstream","origin","HEAD:refs/heads/agora/f2-final-review"]
runtime-available: true
status: "completed"
result-kind: "repository-change"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "local-git-configuration"
created-at: "2026-09-01T17:39:54.443638Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260901t17391788295194z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
