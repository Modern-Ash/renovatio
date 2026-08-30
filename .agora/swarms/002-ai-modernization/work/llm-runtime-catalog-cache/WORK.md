---
schema: "agora/work/v1"
id: "llm-runtime-catalog-cache"
swarm: "ai-modernization"
title: "Real LLM runtime, PromptCatalog, and cache"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"prompt-catalog":"Versioned YAML prompt entries declare promptId, appliesTo, system text, few-shot examples, output schema, validators, and deterministic fallback.","provider-wiring":"A provider-neutral interface has Claude wiring, environment-based credentials, timeout and retry policy, and an offline fake for tests.","cache":"Identical canonical IR input and prompt version resolve to a committed cache entry without a network call.","agora-attribution":"Every cache miss records input hash, output hash, model, promptId, and cache artifact through a governed Agora tool-run without secrets.","service-clarity":"The existing NQL parser service no longer claims to be an LLM integration."}
satisfied-criteria: []
criterion-statuses: {"prompt-catalog":[],"provider-wiring":[],"cache":[],"agora-attribution":[],"service-clarity":[]}
required-artifacts: ["spec","implementation-plan","prompt-catalog","threat-model","test-report"]
child-work-refs: []
budget-limits: null
parent-work: "ai-modernization/three-pass-modernization"
---

# Real LLM runtime, PromptCatalog, and cache

## Description

Queue 4. Depends on annotated-ir-contract. Create renovatio-llm with a provider-neutral client, real Claude API wiring, versioned PromptCatalog entries, strict schemas and validators, temperature zero policy, committed content-addressed cache, and Agora attribution. Rename or narrow the misleading NQL-only LlmIntegrationService.

## Acceptance criteria

- [ ] **prompt-catalog:** Versioned YAML prompt entries declare promptId, appliesTo, system text, few-shot examples, output schema, validators, and deterministic fallback.; stages: none
- [ ] **provider-wiring:** A provider-neutral interface has Claude wiring, environment-based credentials, timeout and retry policy, and an offline fake for tests.; stages: none
- [ ] **cache:** Identical canonical IR input and prompt version resolve to a committed cache entry without a network call.; stages: none
- [ ] **agora-attribution:** Every cache miss records input hash, output hash, model, promptId, and cache artifact through a governed Agora tool-run without secrets.; stages: none
- [ ] **service-clarity:** The existing NQL parser service no longer claims to be an LLM integration.; stages: none

## Required artifacts

- spec
- implementation-plan
- prompt-catalog
- threat-model
- test-report
