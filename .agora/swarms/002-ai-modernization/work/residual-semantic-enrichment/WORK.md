---
schema: "agora/work/v1"
id: "residual-semantic-enrichment"
swarm: "ai-modernization"
title: "Residual LLM semantic enrichment"
state: "verifying"
revision: 2
operational-status: "revalidation"
status-reason: "PR #136 code review found production wiring, annotation idempotency, characterization-baseline binding, domain-review lifecycle, and aggregate evidence traceability gaps."
status-by: "project:owner"
status-at: "2026-08-31T00:53:47.801102Z"
acceptance-criteria: {"domain-language":"Paragraph and data names receive reviewable domain-name and bounded-context suggestions with provenance.","goto-plan":"Irreducible control flow receives a structured plan proposal that cannot apply unless characterization tests remain green.","human-confirmation":"REDEFINES and OCCURS DEPENDING ON interpretations remain suggestions requiring explicit human confirmation.","manual-actions":"Unsupported constructs produce precise explanations and actionable manual migration items.","residual-only":"Supported deterministic constructs never invoke the LLM enrichment path."}
satisfied-criteria: []
criterion-statuses: {"domain-language":[],"goto-plan":[],"human-confirmation":[],"manual-actions":[],"residual-only":[]}
required-artifacts: ["spec","prompt-catalog","test-report"]
child-work-refs: []
budget-limits: null
parent-work: "ai-modernization/three-pass-modernization"
---

# Residual LLM semantic enrichment

## Description

Queue 5. Depends on annotated-ir-contract, llm-runtime-catalog-cache, and characterization-guardrails. Implement only the residual enrichment families: domain naming and bounded contexts, irreducible GO TO restructuring proposals, REDEFINES and OCCURS DEPENDING ON interpretations, and explanations for unsupported constructs.

## Acceptance criteria

- [ ] **domain-language:** Paragraph and data names receive reviewable domain-name and bounded-context suggestions with provenance.; stages: none
- [ ] **goto-plan:** Irreducible control flow receives a structured plan proposal that cannot apply unless characterization tests remain green.; stages: none
- [ ] **human-confirmation:** REDEFINES and OCCURS DEPENDING ON interpretations remain suggestions requiring explicit human confirmation.; stages: none
- [ ] **manual-actions:** Unsupported constructs produce precise explanations and actionable manual migration items.; stages: none
- [ ] **residual-only:** Supported deterministic constructs never invoke the LLM enrichment path.; stages: none

## Required artifacts

- spec
- prompt-catalog
- test-report
