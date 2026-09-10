---
schema: "agora/tool-run/v1"
id: "tool-20260908t22471788918453z"
tool: "github-issues"
operation: "view"
actor: "project:agent"
swarm: "architecture-convergence-2026"
work: "baseline-reconciliation"
environment: null
capability: "issue.read"
risk: "read"
inputs: {"issue":"221"}
command: ["gh","issue","view","221","--json","number,title,body,state,stateReason,labels,assignees,milestone,url,createdAt,updatedAt"]
runtime-available: true
status: "failed"
result-kind: "work-item"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "github-cli-profile"
created-at: "2026-09-08T22:47:33.705552Z"
exit-code: 1
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260908t22471788918453z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
