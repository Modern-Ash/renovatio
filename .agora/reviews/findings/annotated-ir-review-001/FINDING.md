---
schema: "agora/review-finding/v1"
id: "annotated-ir-review-001"
swarm: "ai-modernization"
work: "annotated-ir-contract"
pass: "implementation-review-1"
severity: "high"
status: "resolved"
policy: "acceptance.content-identity"
location: "renovatio-cobol-ir/src/main/java/org/shark/renovatio/cobol/ir/annotated/CobolIrIdentityProjector.java"
created-at: "2026-08-30T17:51:43.348070Z"
decided-by: "project:agent"
decided-at: "2026-08-30T17:57:12.463028Z"
decision-reason: "Identity projection now starts from CobolIntermediateModel, sorts map keys, recursively enumerates data items, level-88 nodes, paragraphs, branches and statements, maps the complete closed expression/condition type set, and fails closed for unmapped Java types. Six focused tests pass."
---

# Review finding annotated-ir-review-001

## Summary

CobolIrIdentityProjector accepts caller-supplied maps and does not exhaustively enumerate or project identity-bearing CobolIntermediateModel node types.
