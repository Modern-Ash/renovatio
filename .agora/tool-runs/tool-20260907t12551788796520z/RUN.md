---
schema: "agora/tool-run/v1"
id: "tool-20260907t12551788796520z"
tool: "repository"
operation: "publish-branch"
actor: "project:agent"
swarm: "renovatio-workbench-domain-model-editor"
work: "domain-model-editor"
environment: null
capability: "repository.write"
risk: "write"
inputs: {"branch":"agora/issue-179-domain-model-editor"}
command: ["git","push","--set-upstream","origin","HEAD:refs/heads/agora/issue-179-domain-model-editor"]
runtime-available: true
status: "completed"
result-kind: "repository-change"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "local-git-configuration"
created-at: "2026-09-07T12:55:20.762868Z"
exit-code: 0
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260907t12551788796520z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
