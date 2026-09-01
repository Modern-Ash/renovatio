---
schema: "agora/work/v1"
id: "f3-architecture-transform"
swarm: "decision-engine-f3"
title: "F3 \u00b7 Architecture as IR-to-TargetModel transformation (issue #148)"
state: "implementing"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"architecture-contract":"A new renovatio-architecture module defines an ArchitectureProfile transformation from SemanticProgram/semantic IR to target-neutral TargetModel selected by profile.architecture.style, without language-emitter coupling.","transaction-script":"TRANSACTION_SCRIPT maps each COBOL program to a service and structured paragraphs to methods while preserving the current default behavior or documenting and accepting any characterization diff.","hexagonal":"HEXAGONAL detects use cases, inbound/outbound ports, adapters, and entities without inventing semantics, and falls back safely when control flow cannot be structured with confidence.","module-grouping":"moduleGrouping groups multi-program fixtures deterministically by domain copybook, prefix, or explicit manual mapping.","suggestions":"Architecture and grouping uncertainty can request governed ARCHITECTURE suggestions through the existing DecisionSuggestionService and ControlFlowPlanGate paths.","target-views":"The Target step preview and architecture diagram are generated from the transformed TargetModel, and preview paths match emitted artifact paths.","verification-scope":"Both Java layouts compile; module grouping and preview parity have automated coverage; issue-122 characterization and reactor/UI regressions pass; no extra styles, generated target tests, or emitter refactor are introduced."}
satisfied-criteria: []
criterion-statuses: {"architecture-contract":["specified","planned"],"transaction-script":["specified","planned"],"hexagonal":["specified","planned"],"module-grouping":["specified","planned"],"suggestions":["specified","planned"],"target-views":["specified","planned"],"verification-scope":["specified","planned"]}
required-artifacts: ["spec"]
child-work-refs: []
budget-limits: null
---

# F3 · Architecture as IR-to-TargetModel transformation (issue #148)

## Description

Introduce renovatio-architecture between semantic analysis and target emission so architecture choices transform neutral semantics into a deterministic TargetModel shared by all emitters, with real preview and diagram views derived from that model.

## Acceptance criteria

- [ ] **architecture-contract:** A new renovatio-architecture module defines an ArchitectureProfile transformation from SemanticProgram/semantic IR to target-neutral TargetModel selected by profile.architecture.style, without language-emitter coupling.; stages: specified, planned
- [ ] **transaction-script:** TRANSACTION_SCRIPT maps each COBOL program to a service and structured paragraphs to methods while preserving the current default behavior or documenting and accepting any characterization diff.; stages: specified, planned
- [ ] **hexagonal:** HEXAGONAL detects use cases, inbound/outbound ports, adapters, and entities without inventing semantics, and falls back safely when control flow cannot be structured with confidence.; stages: specified, planned
- [ ] **module-grouping:** moduleGrouping groups multi-program fixtures deterministically by domain copybook, prefix, or explicit manual mapping.; stages: specified, planned
- [ ] **suggestions:** Architecture and grouping uncertainty can request governed ARCHITECTURE suggestions through the existing DecisionSuggestionService and ControlFlowPlanGate paths.; stages: specified, planned
- [ ] **target-views:** The Target step preview and architecture diagram are generated from the transformed TargetModel, and preview paths match emitted artifact paths.; stages: specified, planned
- [ ] **verification-scope:** Both Java layouts compile; module grouping and preview parity have automated coverage; issue-122 characterization and reactor/UI regressions pass; no extra styles, generated target tests, or emitter refactor are introduced.; stages: specified, planned

## Required artifacts

- spec
