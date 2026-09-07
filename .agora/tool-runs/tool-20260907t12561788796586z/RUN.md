---
schema: "agora/tool-run/v1"
id: "tool-20260907t12561788796586z"
tool: "github-pull-requests"
operation: "checks"
actor: "project:agent"
swarm: "renovatio-workbench-domain-model-editor"
work: "domain-model-editor"
environment: null
capability: "review.read"
risk: "read"
inputs: {"review":"189"}
command: ["gh","pr","checks","189","--json","name,state,link,bucket"]
runtime-available: true
status: "completed"
result-kind: "code-review-checks"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "github-cli-profile"
created-at: "2026-09-07T12:56:26.493110Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260907t12561788796586z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
