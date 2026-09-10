---
schema: "agora/tool-run/v1"
id: "tool-20260909t00131788923591z"
tool: "github-pull-requests"
operation: "checks"
actor: "project:agent"
swarm: "architecture-convergence-2026"
work: "baseline-reconciliation"
environment: null
capability: "review.read"
risk: "read"
inputs: {"review":"239"}
command: ["gh","pr","checks","239","--json","name,state,link,bucket"]
runtime-available: true
status: "completed"
result-kind: "code-review-checks"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "github-cli-profile"
created-at: "2026-09-09T00:13:11.220919Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260909t00131788923591z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
