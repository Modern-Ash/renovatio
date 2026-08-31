---
schema: "agora/review-finding/v1"
id: "review-127-invalid-sidecar-action-item"
swarm: "ai-modernization"
work: "annotated-openrewrite-pass"
pass: "review-20260831"
severity: "high"
status: "resolved"
policy: "fallback"
location: "renovatio-provider-cobol/src/main/java/com/renovatio/provider/cobol/service/JavaGenerationService.java"
created-at: "2026-08-31T12:09:07.601277Z"
decided-by: "project:agent"
decided-at: "2026-08-31T12:15:12.742074Z"
decision-reason: "Commit 2345b650 maps resolver diagnostics to stable manual action items; 163/163 offline tests and the Java 17 network-disabled reactor lane pass"
---

# Review finding review-127-invalid-sidecar-action-item

## Summary

Invalid or stale annotated sidecars fall back deterministically but their resolver diagnostics are not emitted as ManualActionItems
