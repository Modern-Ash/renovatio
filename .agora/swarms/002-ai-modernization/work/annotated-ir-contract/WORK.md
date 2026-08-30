---
schema: "agora/work/v1"
id: "annotated-ir-contract"
swarm: "ai-modernization"
title: "Versioned annotated IR sidecar contract"
state: "implementing"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"model":"AnnotatedCobolModel preserves the base IR and carries typed annotations without mutating source semantic nodes.","sidecar-schema":"A versioned strict schema validates committed *.annotated.json sidecars and rejects unknown or malformed annotations.","content-identity":"Canonical node hashes plus prompt versions define stable content-addressed identities for annotations and cache entries.","context-seam":"The existing ExecutionContext seam can carry the annotated model without introducing provider calls into recipes."}
satisfied-criteria: []
criterion-statuses: {"model":["specified","planned"],"sidecar-schema":["specified","planned"],"content-identity":["specified","planned"],"context-seam":["specified","planned"]}
required-artifacts: ["spec","json-schema","architecture-decision-record"]
child-work-refs: []
budget-limits: null
parent-work: "ai-modernization/three-pass-modernization"
---

# Versioned annotated IR sidecar contract

## Description

Queue 3. May run in parallel with deterministic-semantic-core after fixture conventions are stable. Define AnnotatedCobolModel as an additive sidecar contract over CobolIntermediateModel, including node identity, provenance, confidence, review state, and strict JSON Schema.

## Acceptance criteria

- [ ] **model:** AnnotatedCobolModel preserves the base IR and carries typed annotations without mutating source semantic nodes.; stages: specified, planned
- [ ] **sidecar-schema:** A versioned strict schema validates committed *.annotated.json sidecars and rejects unknown or malformed annotations.; stages: specified, planned
- [ ] **content-identity:** Canonical node hashes plus prompt versions define stable content-addressed identities for annotations and cache entries.; stages: specified, planned
- [ ] **context-seam:** The existing ExecutionContext seam can carry the annotated model without introducing provider calls into recipes.; stages: specified, planned

## Required artifacts

- spec
- json-schema
- architecture-decision-record
