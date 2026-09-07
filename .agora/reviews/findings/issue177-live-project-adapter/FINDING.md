---
schema: "agora/review-finding/v1"
id: "issue177-live-project-adapter"
swarm: "renovatio-workbench"
work: "issue-177-theia-ide-shell"
pass: "ide-shell-technical-review"
severity: "medium"
status: "open"
policy: "project-navigation"
location: "renovatio-workbench/extensions/renovatio-core-ui/src/browser/renovatio-shell-widget.tsx"
created-at: "2026-09-06T02:30:48.671859Z"
decided-by: null
decided-at: null
decision-reason: null
---

# Review finding issue177-live-project-adapter

## Summary

Project navigation uses a deterministic shell fixture; live backend data requires a separately reviewed adapter and authorization policy.
