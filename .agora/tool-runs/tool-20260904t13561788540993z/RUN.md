---
schema: "agora/tool-run/v1"
id: "tool-20260904t13561788540993z"
tool: "github-issues"
operation: "view"
actor: "project:agent"
swarm: "decision-engine-f8-review-fixes"
work: "f8-review-fixes"
environment: null
capability: "issue.read"
risk: "read"
inputs: {"issue":"152"}
command: ["gh","issue","view","152","--json","number,title,body,state,stateReason,labels,assignees,milestone,url,createdAt,updatedAt"]
runtime-available: true
status: "completed"
result-kind: "work-item"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "github-cli-profile"
created-at: "2026-09-04T13:56:33.133314Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260904t13561788540993z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
