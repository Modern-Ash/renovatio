---
schema: "agora/tool-run/v1"
id: "tool-20260904t14251788542700z"
tool: "github-pull-requests"
operation: "checks"
actor: "project:agent"
swarm: "decision-engine-epic-gaps"
work: "epic-cli-gaps"
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
created-at: "2026-09-04T14:25:00.187456Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260904t14251788542700z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
