---
schema: "agora/review-finding/v1"
id: "annotation-identity-conflict"
swarm: "ai-modernization"
work: "residual-semantic-enrichment"
pass: "pr-136-review"
severity: "high"
status: "resolved"
policy: "annotated-ir-identity-v1"
location: "renovatio-llm/src/main/java/org/shark/renovatio/llm/residual/ResidualAnnotationAssembler.java"
created-at: "2026-08-31T00:54:00.415653Z"
decided-by: "project:agent"
decided-at: "2026-08-31T01:03:19.572891Z"
decision-reason: "Commit f1d143f deduplicates an existing identity with the same output hash and rejects a conflicting output hash; regression coverage passes."
---

# Review finding annotation-identity-conflict

## Summary

ResidualAnnotationAssembler appends duplicate annotation identities instead of deduplicating identical proposals or rejecting conflicting output hashes.
