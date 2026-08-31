---
schema: "agora/work/v1"
id: "residual-semantic-enrichment"
swarm: "ai-modernization"
title: "Residual LLM semantic enrichment"
state: "implementing"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"domain-language":"Paragraph and data names receive reviewable domain-name and bounded-context suggestions with provenance.","goto-plan":"Irreducible control flow receives a structured plan proposal that cannot apply unless characterization tests remain green.","human-confirmation":"REDEFINES and OCCURS DEPENDING ON interpretations remain suggestions requiring explicit human confirmation.","manual-actions":"Unsupported constructs produce precise explanations and actionable manual migration items.","residual-only":"Supported deterministic constructs never invoke the LLM enrichment path."}
satisfied-criteria: []
criterion-statuses: {"domain-language":["specified","planned","implemented"],"goto-plan":["specified","planned"],"human-confirmation":["specified","planned"],"manual-actions":["specified","planned"],"residual-only":["specified","planned","implemented"]}
required-artifacts: ["spec","prompt-catalog","test-report"]
child-work-refs: []
budget-limits: null
parent-work: "ai-modernization/three-pass-modernization"
---

# Residual LLM semantic enrichment

## Description

Queue 5. Depends on annotated-ir-contract, llm-runtime-catalog-cache, and characterization-guardrails. Implement only the residual enrichment families: domain naming and bounded contexts, irreducible GO TO restructuring proposals, REDEFINES and OCCURS DEPENDING ON interpretations, and explanations for unsupported constructs.

## Acceptance criteria

- [ ] **domain-language:** Paragraph and data names receive reviewable domain-name and bounded-context suggestions with provenance.; stages: specified, planned, implemented
- [ ] **goto-plan:** Irreducible control flow receives a structured plan proposal that cannot apply unless characterization tests remain green.; stages: specified, planned
- [ ] **human-confirmation:** REDEFINES and OCCURS DEPENDING ON interpretations remain suggestions requiring explicit human confirmation.; stages: specified, planned
- [ ] **manual-actions:** Unsupported constructs produce precise explanations and actionable manual migration items.; stages: specified, planned
- [ ] **residual-only:** Supported deterministic constructs never invoke the LLM enrichment path.; stages: specified, planned, implemented

## Required artifacts

- spec
- prompt-catalog
- test-report
