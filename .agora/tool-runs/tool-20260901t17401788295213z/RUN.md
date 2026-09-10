---
schema: "agora/tool-run/v1"
id: "tool-20260901t17401788295213z"
tool: "github-pull-requests"
operation: "create"
actor: "project:agent"
swarm: "decision-engine-f2"
work: "f2-semantic-ir-emitter-spi"
environment: null
capability: "review.write"
risk: "write"
inputs: {"project":"Modern-Ash/renovatio","base":"main","head":"agora/f2-final-review","title":"fix(decision-engine): close final F2 review gaps","description":"Follow-up to #159 addressing the two post-merge review findings.\n\n- defer control-break persistence until aggregate validation succeeds\n- include accepted annotation output hashes in semantic provenance\n- add focused regressions for partial writes and provenance identity\n\nVerification: 356 tests passed across the F2 reactor, API, MCP, and CLI."}
command: ["gh","pr","create","--repo","Modern-Ash/renovatio","--base","main","--head","agora/f2-final-review","--title","fix(decision-engine): close final F2 review gaps","--body","Follow-up to #159 addressing the two post-merge review findings.\n\n- defer control-break persistence until aggregate validation succeeds\n- include accepted annotation output hashes in semantic provenance\n- add focused regressions for partial writes and provenance identity\n\nVerification: 356 tests passed across the F2 reactor, API, MCP, and CLI."]
runtime-available: true
status: "completed"
result-kind: "code-review"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "github-cli-profile"
created-at: "2026-09-01T17:40:13.576381Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260901t17401788295213z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
