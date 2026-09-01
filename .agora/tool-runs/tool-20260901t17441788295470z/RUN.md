---
schema: "agora/tool-run/v1"
id: "tool-20260901t17441788295470z"
tool: "github-pull-requests"
operation: "view"
actor: "project:agent"
swarm: "decision-engine-f2"
work: "f2-semantic-ir-emitter-spi"
environment: null
capability: "review.read"
risk: "read"
inputs: {"review":"160"}
command: ["gh","pr","view","160","--json","number,title,body,state,isDraft,mergeable,reviewDecision,headRefName,baseRefName,url,author,reviews,statusCheckRollup"]
runtime-available: true
status: "completed"
result-kind: "code-review"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "github-cli-profile"
created-at: "2026-09-01T17:44:30.172350Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260901t17441788295470z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
