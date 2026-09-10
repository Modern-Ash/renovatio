---
schema: "agora/tool-run/v1"
id: "tool-20260908t23051788919519z"
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
created-at: "2026-09-08T23:05:19.408931Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260908t23051788919519z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
