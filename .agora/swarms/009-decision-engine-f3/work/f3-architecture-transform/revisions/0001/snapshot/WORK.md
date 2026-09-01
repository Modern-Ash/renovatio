---
schema: "agora/work/v1"
id: "f3-architecture-transform"
swarm: "decision-engine-f3"
title: "F3 \u00b7 Architecture as IR-to-TargetModel transformation (issue #148)"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"architecture-contract":"A new renovatio-architecture module defines an ArchitectureProfile transformation from SemanticProgram/semantic IR to target-neutral TargetModel selected by profile.architecture.style, without language-emitter coupling.","transaction-script":"TRANSACTION_SCRIPT maps each COBOL program to a service and structured paragraphs to methods while preserving the current default behavior or documenting and accepting any characterization diff.","hexagonal":"HEXAGONAL detects use cases, inbound/outbound ports, adapters, and entities without inventing semantics, and falls back safely when control flow cannot be structured with confidence.","module-grouping":"moduleGrouping groups multi-program fixtures deterministically by domain copybook, prefix, or explicit manual mapping.","suggestions":"Architecture and grouping uncertainty can request governed ARCHITECTURE suggestions through the existing DecisionSuggestionService and ControlFlowPlanGate paths.","target-views":"The Target step preview and architecture diagram are generated from the transformed TargetModel, and preview paths match emitted artifact paths.","verification-scope":"Both Java layouts compile; module grouping and preview parity have automated coverage; issue-122 characterization and reactor/UI regressions pass; no extra styles, generated target tests, or emitter refactor are introduced."}
satisfied-criteria: ["architecture-contract","transaction-script","hexagonal","module-grouping","suggestions","target-views","verification-scope"]
criterion-statuses: {"architecture-contract":["specified","planned","implemented","verified","accepted"],"transaction-script":["specified","planned","implemented","verified","accepted"],"hexagonal":["specified","planned","implemented","verified","accepted"],"module-grouping":["specified","planned","implemented","verified","accepted"],"suggestions":["specified","planned","implemented","verified","accepted"],"target-views":["specified","planned","implemented","verified","accepted"],"verification-scope":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec"]
child-work-refs: []
budget-limits: null
---

# F3 · Architecture as IR-to-TargetModel transformation (issue #148)

## Description

Introduce renovatio-architecture between semantic analysis and target emission so architecture choices transform neutral semantics into a deterministic TargetModel shared by all emitters, with real preview and diagram views derived from that model.

## Acceptance criteria

- [x] **architecture-contract:** A new renovatio-architecture module defines an ArchitectureProfile transformation from SemanticProgram/semantic IR to target-neutral TargetModel selected by profile.architecture.style, without language-emitter coupling.; stages: specified, planned, implemented, verified, accepted
- [x] **transaction-script:** TRANSACTION_SCRIPT maps each COBOL program to a service and structured paragraphs to methods while preserving the current default behavior or documenting and accepting any characterization diff.; stages: specified, planned, implemented, verified, accepted
- [x] **hexagonal:** HEXAGONAL detects use cases, inbound/outbound ports, adapters, and entities without inventing semantics, and falls back safely when control flow cannot be structured with confidence.; stages: specified, planned, implemented, verified, accepted
- [x] **module-grouping:** moduleGrouping groups multi-program fixtures deterministically by domain copybook, prefix, or explicit manual mapping.; stages: specified, planned, implemented, verified, accepted
- [x] **suggestions:** Architecture and grouping uncertainty can request governed ARCHITECTURE suggestions through the existing DecisionSuggestionService and ControlFlowPlanGate paths.; stages: specified, planned, implemented, verified, accepted
- [x] **target-views:** The Target step preview and architecture diagram are generated from the transformed TargetModel, and preview paths match emitted artifact paths.; stages: specified, planned, implemented, verified, accepted
- [x] **verification-scope:** Both Java layouts compile; module grouping and preview parity have automated coverage; issue-122 characterization and reactor/UI regressions pass; no extra styles, generated target tests, or emitter refactor are introduced.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
