---
schema: "agora/review-finding/v1"
id: "review-139-source-path"
swarm: "ai-modernization"
work: "annotated-openrewrite-pass"
pass: "pr-139-review"
severity: "medium"
status: "resolved"
policy: "fallback"
location: "renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/translation/CobolSemanticTranspiler.java"
created-at: "2026-08-31T12:45:13.853364Z"
decided-by: "project:agent"
decided-at: "2026-08-31T12:50:25.386229Z"
decision-reason: "Commit 294e091 propagates the actual orchestration source path and verifies a nested .cbl path in action items; 167/167 offline tests and the Java 17 network-disabled reactor lane pass"
---

# Review finding review-139-source-path

## Summary

Dropped annotation action items synthesize PROGRAM-ID.cob instead of preserving the actual source path
