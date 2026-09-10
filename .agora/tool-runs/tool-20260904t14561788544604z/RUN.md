---
schema: "agora/tool-run/v1"
id: "tool-20260904t14561788544604z"
tool: "github-pull-requests"
operation: "checks"
actor: "project:agent"
swarm: "decision-engine-node-multiprogram"
work: "node-multiprogram-generation"
environment: null
capability: "review.read"
risk: "read"
inputs: {"review":"168"}
command: ["gh","pr","checks","168","--json","name,state,link,bucket"]
runtime-available: true
status: "completed"
result-kind: "code-review-checks"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "github-cli-profile"
created-at: "2026-09-04T14:56:44.595442Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260904t14561788544604z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
