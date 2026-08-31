---
schema: "agora/review-finding/v1"
id: "review-127-invalid-sidecar-action-item"
swarm: "ai-modernization"
work: "annotated-openrewrite-pass"
pass: "review-20260831"
severity: "high"
status: "open"
policy: "fallback"
location: "renovatio-provider-cobol/src/main/java/com/renovatio/provider/cobol/service/JavaGenerationService.java"
created-at: "2026-08-31T12:09:07.601277Z"
decided-by: null
decided-at: null
decision-reason: null
---

# Review finding review-127-invalid-sidecar-action-item

## Summary

Invalid or stale annotated sidecars fall back deterministically but their resolver diagnostics are not emitted as ManualActionItems
