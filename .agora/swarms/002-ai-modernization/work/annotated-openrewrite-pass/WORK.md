---
schema: "agora/work/v1"
id: "annotated-openrewrite-pass"
swarm: "ai-modernization"
title: "Deterministic OpenRewrite pass over annotated IR"
state: "verifying"
revision: 2
operational-status: "revalidation"
status-reason: "Address five confirmed review findings on PR #139"
status-by: "project:owner"
status-at: "2026-08-31T12:44:59.859602Z"
acceptance-criteria: {"annotated-consumption":"CobolSemanticTranspiler injects the validated annotated model and recipes read it through the existing context key seam.","ast-safe":"Recipes apply only schema-approved annotations using AST-safe deterministic transformations.","no-provider-call":"Recipe execution contains no provider client, credential, network, or prompt dependency.","reproducible":"Committed sidecars and cache artifacts reproduce identical generated sources in offline CI.","fallback":"Missing, rejected, or stale annotations use deterministic translation and emit action items."}
satisfied-criteria: []
criterion-statuses: {"annotated-consumption":["specified","planned","implemented","verified"],"ast-safe":["specified","planned","implemented","verified"],"no-provider-call":["specified","planned","implemented","verified"],"reproducible":["specified","planned","implemented","verified"],"fallback":["specified","planned","implemented","verified"]}
required-artifacts: ["implementation-plan","test-report"]
child-work-refs: []
budget-limits: null
parent-work: "ai-modernization/three-pass-modernization"
---

# Deterministic OpenRewrite pass over annotated IR

## Description

Queue 6. Depends on deterministic-semantic-core, annotated-ir-contract, and residual-semantic-enrichment. Update CobolSemanticTranspiler and PopulateCobolProcessRecipe to consume validated AnnotatedCobolModel through ExecutionContext while keeping recipes pure and AST-safe.

## Acceptance criteria

- [ ] **annotated-consumption:** CobolSemanticTranspiler injects the validated annotated model and recipes read it through the existing context key seam.; stages: specified, planned, implemented, verified
- [ ] **ast-safe:** Recipes apply only schema-approved annotations using AST-safe deterministic transformations.; stages: specified, planned, implemented, verified
- [ ] **no-provider-call:** Recipe execution contains no provider client, credential, network, or prompt dependency.; stages: specified, planned, implemented, verified
- [ ] **reproducible:** Committed sidecars and cache artifacts reproduce identical generated sources in offline CI.; stages: specified, planned, implemented, verified
- [ ] **fallback:** Missing, rejected, or stale annotations use deterministic translation and emit action items.; stages: specified, planned, implemented, verified

## Required artifacts

- implementation-plan
- test-report
