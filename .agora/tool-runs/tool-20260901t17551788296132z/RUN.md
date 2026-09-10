---
schema: "agora/tool-run/v1"
id: "tool-20260901t17551788296132z"
tool: "github-issues"
operation: "view"
actor: "project:agent"
swarm: "decision-engine-f3"
work: "f3-architecture-transform"
environment: null
capability: "issue.read"
risk: "read"
inputs: {"issue":"147"}
command: ["gh","issue","view","147","--json","number,title,body,state,stateReason,labels,assignees,milestone,url,createdAt,updatedAt"]
runtime-available: true
status: "completed"
result-kind: "work-item"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "github-cli-profile"
created-at: "2026-09-01T17:55:32.384001Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260901t17551788296132z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
