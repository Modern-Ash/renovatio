---
schema: "agora/tool-run/v1"
id: "tool-20260908t22461788918398z"
tool: "repository"
operation: "create-branch"
actor: "project:agent"
swarm: "architecture-convergence-2026"
work: "baseline-reconciliation"
environment: null
capability: "repository.write"
risk: "write"
inputs: {"branch":"agora/issue-222-baseline-reconciliation"}
command: ["git","checkout","-b","agora/issue-222-baseline-reconciliation"]
runtime-available: true
status: "failed"
result-kind: "repository-change"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "local-git-configuration"
created-at: "2026-09-08T22:46:38.155290Z"
exit-code: 128
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260908t22461788918398z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
