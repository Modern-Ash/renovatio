---
schema: "agora/work/v1"
id: "annotated-openrewrite-pass"
swarm: "ai-modernization"
title: "Deterministic OpenRewrite pass over annotated IR"
state: "completed"
revision: 2
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"annotated-consumption":"CobolSemanticTranspiler injects the validated annotated model and recipes read it through the existing context key seam.","ast-safe":"Recipes apply only schema-approved annotations using AST-safe deterministic transformations.","no-provider-call":"Recipe execution contains no provider client, credential, network, or prompt dependency.","reproducible":"Committed sidecars and cache artifacts reproduce identical generated sources in offline CI.","fallback":"Missing, rejected, or stale annotations use deterministic translation and emit action items."}
satisfied-criteria: ["annotated-consumption","ast-safe","no-provider-call","reproducible","fallback"]
criterion-statuses: {"annotated-consumption":["specified","planned","implemented","verified","accepted"],"ast-safe":["specified","planned","implemented","verified","accepted"],"no-provider-call":["specified","planned","implemented","verified","accepted"],"reproducible":["specified","planned","implemented","verified","accepted"],"fallback":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["implementation-plan","test-report"]
child-work-refs: []
budget-limits: null
parent-work: "ai-modernization/three-pass-modernization"
---

# Deterministic OpenRewrite pass over annotated IR

## Description

Queue 6. Depends on deterministic-semantic-core, annotated-ir-contract, and residual-semantic-enrichment. Update CobolSemanticTranspiler and PopulateCobolProcessRecipe to consume validated AnnotatedCobolModel through ExecutionContext while keeping recipes pure and AST-safe.

## Acceptance criteria

- [x] **annotated-consumption:** CobolSemanticTranspiler injects the validated annotated model and recipes read it through the existing context key seam.; stages: specified, planned, implemented, verified, accepted
- [x] **ast-safe:** Recipes apply only schema-approved annotations using AST-safe deterministic transformations.; stages: specified, planned, implemented, verified, accepted
- [x] **no-provider-call:** Recipe execution contains no provider client, credential, network, or prompt dependency.; stages: specified, planned, implemented, verified, accepted
- [x] **reproducible:** Committed sidecars and cache artifacts reproduce identical generated sources in offline CI.; stages: specified, planned, implemented, verified, accepted
- [x] **fallback:** Missing, rejected, or stale annotations use deterministic translation and emit action items.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- implementation-plan
- test-report
