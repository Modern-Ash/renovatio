---
schema: "agora/tool-run/v1"
id: "tool-20260908t12431788882190z"
tool: "github-issues"
operation: "search"
actor: "project:owner"
swarm: "architecture-convergence-2026"
work: null
environment: null
capability: "issue.read"
risk: "read"
inputs: {"query":"repo:Modern-Ash/renovatio is:issue architecture convergence in:title,body"}
command: ["gh","search","issues","repo:Modern-Ash/renovatio is:issue architecture convergence in:title,body","--limit","50","--json","number,title,state,url,repository,updatedAt"]
runtime-available: true
status: "failed"
result-kind: "work-item-list"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "github-cli-profile"
created-at: "2026-09-08T12:43:10.939301Z"
exit-code: 1
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260908t12431788882190z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
