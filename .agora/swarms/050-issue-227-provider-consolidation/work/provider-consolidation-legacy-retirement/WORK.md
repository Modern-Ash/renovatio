---
schema: "agora/work/v1"
id: "provider-consolidation-legacy-retirement"
swarm: "issue-227-provider-consolidation"
title: "AC-06: Consolidate providers and retire legacy generation"
state: "verifying"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"single-authority":"Exactly one authoritative provider exists per language/capability key and startup fails diagnostically on duplicates.","provider-cleanup":"Java and COBOL providers are consolidated with compatibility adapters only where required.","dependency-direction":"renovatio-provider-cobol does not depend on renovatio-provider-java or web controllers and communicates through Semantic IR and application ports.","service-decomposition":"Generation responsibilities are separated across parse, project, plan, render, orchestration, and write collaborators.","legacy-route":"Legacy routing is removed or isolated behind an explicit compatibility bridge.","emitter-registration":"JavaEmitter and NodeEmitter use the shared registry lifecycle and source providers do not instantiate emitters.","logging":"Production System.out usage is removed in favor of structured logging without sensitive source data."}
satisfied-criteria: []
criterion-statuses: {"single-authority":["specified","planned","implemented","verified"],"provider-cleanup":["specified","planned","implemented","verified"],"dependency-direction":["specified","planned","implemented","verified"],"service-decomposition":["specified","planned","implemented","verified"],"legacy-route":["specified","planned","implemented","verified"],"emitter-registration":["specified","planned","implemented","verified"],"logging":["specified","planned","implemented","verified"]}
required-artifacts: ["spec","implementation-plan","deprecation-plan","architecture-report","test-report"]
child-work-refs: []
budget-limits: null
---

# AC-06: Consolidate providers and retire legacy generation

## Description

Retrospective governance reconstruction for issue #227. Implementation commits d611fe08 and 24bf05d3, PR #247 review remediation, CI, merge commit 6ebf24f, and automatic issue closure existed before this durable work record.

## Acceptance criteria

- [ ] **single-authority:** Exactly one authoritative provider exists per language/capability key and startup fails diagnostically on duplicates.; stages: specified, planned, implemented, verified
- [ ] **provider-cleanup:** Java and COBOL providers are consolidated with compatibility adapters only where required.; stages: specified, planned, implemented, verified
- [ ] **dependency-direction:** renovatio-provider-cobol does not depend on renovatio-provider-java or web controllers and communicates through Semantic IR and application ports.; stages: specified, planned, implemented, verified
- [ ] **service-decomposition:** Generation responsibilities are separated across parse, project, plan, render, orchestration, and write collaborators.; stages: specified, planned, implemented, verified
- [ ] **legacy-route:** Legacy routing is removed or isolated behind an explicit compatibility bridge.; stages: specified, planned, implemented, verified
- [ ] **emitter-registration:** JavaEmitter and NodeEmitter use the shared registry lifecycle and source providers do not instantiate emitters.; stages: specified, planned, implemented, verified
- [ ] **logging:** Production System.out usage is removed in favor of structured logging without sensitive source data.; stages: specified, planned, implemented, verified

## Required artifacts

- spec
- implementation-plan
- deprecation-plan
- architecture-report
- test-report
