---
schema: "agora/review-finding/v1"
id: "annotated-ir-review-003"
swarm: "ai-modernization"
work: "annotated-ir-contract"
pass: "implementation-review-1"
severity: "high"
status: "resolved"
policy: "acceptance.content-identity"
location: "renovatio-cobol-ir/src/main/java/org/shark/renovatio/cobol/ir/annotated/AnnotatedCobolValidator.java"
created-at: "2026-08-30T17:51:43.791396Z"
decided-by: "project:agent"
decided-at: "2026-08-30T17:52:31.892711Z"
decision-reason: "Validator now recomputes annotationId and outputHash from normative canonical projections; focused regression suite passes 3/3."
---

# Review finding annotated-ir-review-003

## Summary

Semantic validation does not recompute annotationId and outputHash from their normative canonical projections, so malformed content identities can pass.
