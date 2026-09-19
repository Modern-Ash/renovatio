---
schema: "agora/tool-run/v1"
id: "tool-20260910t14081789060082z"
tool: "repository"
operation: "publish-branch"
actor: "project:agent"
swarm: "issue-228-cobol-java-reference-path"
work: "cobol-java-reference-path"
environment: null
capability: "repository.write"
risk: "write"
inputs: {"branch":"chore/issue-228-agora-closure"}
command: ["git","push","--set-upstream","origin","HEAD:refs/heads/chore/issue-228-agora-closure"]
runtime-available: true
status: "failed"
result-kind: "repository-change"
timeout-seconds: 300
max-output-bytes: 1048576
authentication-reference: "local-git-configuration"
created-at: "2026-09-10T14:08:02.897959Z"
exit-code: 128
authentication-verified: false
authentication-fingerprint: null
authentication-public-key: null
authorization-sha256: null
authorization-signature: null
---

# Tool run tool-20260910t14081789060082z

This record contains invocation metadata, not credentials. Authentication is resolved by the external executable and its environment.
