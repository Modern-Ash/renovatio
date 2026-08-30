---
schema: "agora/work/v1"
id: "annotated-ir-contract"
swarm: "ai-modernization"
title: "Versioned annotated IR sidecar contract"
state: "verifying"
revision: 2
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"model":"AnnotatedCobolModel preserves the base IR and carries typed annotations without mutating source semantic nodes.","sidecar-schema":"A versioned strict schema validates committed *.annotated.json sidecars and rejects unknown or malformed annotations.","content-identity":"Canonical node hashes plus prompt versions define stable content-addressed identities for annotations and cache entries.","context-seam":"The existing ExecutionContext seam can carry the annotated model without introducing provider calls into recipes."}
satisfied-criteria: []
criterion-statuses: {"model":["specified","planned","implemented","verified"],"sidecar-schema":["specified","planned","implemented","verified"],"content-identity":["specified","planned","implemented","verified"],"context-seam":["specified","planned","implemented","verified"]}
required-artifacts: ["spec","json-schema","architecture-decision-record"]
child-work-refs: []
budget-limits: null
parent-work: "ai-modernization/three-pass-modernization"
---

# Versioned annotated IR sidecar contract

## Description

Queue 3. May run in parallel with deterministic-semantic-core after fixture conventions are stable. Define AnnotatedCobolModel as an additive sidecar contract over CobolIntermediateModel, including node identity, provenance, confidence, review state, and strict JSON Schema.

## Acceptance criteria

- [ ] **model:** AnnotatedCobolModel preserves the base IR and carries typed annotations without mutating source semantic nodes.; stages: specified, planned, implemented, verified
- [ ] **sidecar-schema:** A versioned strict schema validates committed *.annotated.json sidecars and rejects unknown or malformed annotations.; stages: specified, planned, implemented, verified
- [ ] **content-identity:** Canonical node hashes plus prompt versions define stable content-addressed identities for annotations and cache entries.; stages: specified, planned, implemented, verified
- [ ] **context-seam:** The existing ExecutionContext seam can carry the annotated model without introducing provider calls into recipes.; stages: specified, planned, implemented, verified

## Required artifacts

- spec
- json-schema
- architecture-decision-record
