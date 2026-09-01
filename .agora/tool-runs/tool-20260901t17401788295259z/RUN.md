---
schema: "agora/tool-run/v1"
id: "tool-20260901t17401788295259z"
tool: "github-pull-requests"
operation: "checks"
actor: "project:agent"
swarm: "decision-engine-f2"
work: "f2-semantic-ir-emitter-spi"
environment: null
capability: "review.read"
risk: "read"
inputs: {"review":"160"}
command: ["gh","pr","checks","160","--json","name,state,link,bucket"]
runtime-available: true
status: "completed"
result-kind: "code-review-checks"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "github-cli-profile"
created-at: "2026-09-01T17:40:59.312961Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260901t17401788295259z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
