---
schema: "agora/tool-run/v1"
id: "tool-20260907t00141788750844z"
tool: "repository"
operation: "publish-branch"
actor: "project:agent"
swarm: "equivalence-rework"
work: "deterministic-equivalence-harness"
environment: null
capability: "repository.write"
risk: "write"
inputs: {"branch":"agora/renovatio-workbench"}
command: ["git","push","--set-upstream","origin","HEAD:refs/heads/agora/renovatio-workbench"]
runtime-available: true
status: "completed"
result-kind: "repository-change"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "local-git-configuration"
created-at: "2026-09-07T00:14:04.372420Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260907t00141788750844z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
