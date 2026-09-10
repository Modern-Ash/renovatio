---
schema: "agora/review-finding/v1"
id: "annotated-ir-review-002"
swarm: "ai-modernization"
work: "annotated-ir-contract"
pass: "implementation-review-1"
severity: "high"
status: "resolved"
policy: "acceptance.sidecar-schema"
location: "renovatio-provider-cobol/src/test/java/org/shark/renovatio/provider/cobol/guardrail/GuardrailSchemaCatalogTest.java"
created-at: "2026-08-30T17:51:43.571949Z"
decided-by: "project:agent"
decided-at: "2026-08-30T17:54:57.484191Z"
decision-reason: "Committed valid and invalid fixtures are evaluated by the NetworkNT JSON Schema 2020-12 validator; the focused regression suite passes."
---

# Review finding annotated-ir-review-002

## Summary

Schema tests inspect schema structure but do not validate committed positive and negative *.annotated.json fixtures against JSON Schema 2020-12.
