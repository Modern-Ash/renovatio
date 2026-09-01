---
schema: "agora/tool-operation/v1"
id: "publish-branch"
name: "Publish the current branch"
capability: "repository.write"
risk: "write"
arguments: ["push","--set-upstream","origin","HEAD:refs/heads/{branch}"]
inputs: ["branch"]
result-kind: "repository-change"
---

# Publish the current branch

Publishes the current `HEAD` to one explicitly named branch on `origin` and records upstream
tracking. The operation does not force-update, delete, or publish tags.
