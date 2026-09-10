---
schema: "agora/tool-run/v1"
id: "tool-20260908t23291788920957z"
tool: "github-pull-requests"
operation: "comment"
actor: "project:agent"
swarm: "architecture-convergence-2026"
work: "baseline-reconciliation"
environment: null
capability: "review.write"
risk: "write"
inputs: {"review":"239","body":"@codex review"}
command: ["gh","pr","comment","239","--body","@codex review"]
runtime-available: true
status: "completed"
result-kind: "code-review-comment"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "github-cli-profile"
created-at: "2026-09-08T23:29:17.685284Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260908t23291788920957z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
