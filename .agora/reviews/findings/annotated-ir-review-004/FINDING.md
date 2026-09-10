---
schema: "agora/review-finding/v1"
id: "annotated-ir-review-004"
swarm: "ai-modernization"
work: "annotated-ir-contract"
pass: "implementation-review-2"
severity: "high"
status: "resolved"
policy: "acceptance.content-identity"
location: "renovatio-cobol-ir/src/main/java/org/shark/renovatio/cobol/ir/annotated/CobolIrIdentityProjector.java"
created-at: "2026-08-30T17:58:46.260780Z"
decided-by: "project:agent"
decided-at: "2026-08-30T18:00:46.407116Z"
decision-reason: "The dedicated projection now includes every CobolIntermediateModel property with explicit serializers for CFG, execution context, control-break patterns, decomposed logic and nested records; hash-sensitivity regression passes and the clean 194-test reactor is green."
---

# Review finding annotated-ir-review-004

## Summary

The dedicated CobolIntermediateModel projection omits controlFlowGraph, executionContext, controlBreakPatterns, and decomposedLogic, so baseIrHash does not identify the complete schema-valid base IR.
