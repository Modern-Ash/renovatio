---
schema: "agora/review-finding/v1"
id: "annotation-identity-conflict"
swarm: "ai-modernization"
work: "residual-semantic-enrichment"
pass: "pr-136-review"
severity: "high"
status: "open"
policy: "annotated-ir-identity-v1"
location: "renovatio-llm/src/main/java/org/shark/renovatio/llm/residual/ResidualAnnotationAssembler.java"
created-at: "2026-08-31T00:54:00.415653Z"
decided-by: null
decided-at: null
decision-reason: null
---

# Review finding annotation-identity-conflict

## Summary

ResidualAnnotationAssembler appends duplicate annotation identities instead of deduplicating identical proposals or rejecting conflicting output hashes.
