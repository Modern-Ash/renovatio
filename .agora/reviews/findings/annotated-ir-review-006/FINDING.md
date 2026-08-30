---
schema: "agora/review-finding/v1"
id: "annotated-ir-review-006"
swarm: "ai-modernization"
work: "annotated-ir-contract"
pass: "github-review-pr-134"
severity: "high"
status: "resolved"
policy: "acceptance.context-seam"
location: "renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/translation/CobolSemanticTranspiler.java"
created-at: "2026-08-30T18:10:45.203128Z"
decided-by: "project:agent"
decided-at: "2026-08-30T18:11:36.757736Z"
decision-reason: "The transpiler now recomputes the exact base hash and node index, runs AnnotatedCobolValidator, checks base IR version, and omits invalid annotated contexts; valid and stale-context regressions pass."
---

# Review finding annotated-ir-review-006

## Summary

CobolSemanticTranspiler injected AnnotatedCobolContext without validating its sidecar against the exact base model.
