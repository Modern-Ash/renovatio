---
schema: "agora/tool-run/v1"
id: "tool-20260904t14541788544489z"
tool: "repository"
operation: "publish-branch"
actor: "project:agent"
swarm: "decision-engine-node-multiprogram"
work: "node-multiprogram-generation"
environment: null
capability: "repository.write"
risk: "write"
inputs: {"branch":"fix/f8-review-findings"}
command: ["git","push","--set-upstream","origin","HEAD:refs/heads/fix/f8-review-findings"]
runtime-available: true
status: "completed"
result-kind: "repository-change"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "local-git-configuration"
created-at: "2026-09-04T14:54:49.299144Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260904t14541788544489z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
