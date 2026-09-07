---
schema: "agora/work/v1"
id: "issue-173-llm-eval-methodology"
swarm: "llm-eval-methodology"
title: "LLM evaluation methodology for domain and architecture modeling"
state: "verifying"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"schema-validity":"Evaluation cases and sample outputs validate against versioned schemas and all IR references resolve.","critical-regressions":"Critical fixtures have baseline goldens and regression gates fail on invalid, hallucinated, or unsafe outputs.","comparable-reports":"Every prompt/model/version run emits a deterministic report comparable with a baseline.","human-review-boundary":"LLM suggestions are represented as reviewable decisions and never promoted as final code.","metrics":"Reports include acceptance, fallback, cost, latency, and cache-hit metrics fields.","agora-evidence":"Specification, implementation plan, verification report, and test evidence are registered in Agora."}
satisfied-criteria: []
criterion-statuses: {"schema-validity":["specified","planned","implemented","verified"],"critical-regressions":["specified","planned","implemented","verified"],"comparable-reports":["specified","planned","implemented","verified"],"human-review-boundary":["specified","planned","implemented","verified"],"metrics":["specified","planned","implemented","verified"],"agora-evidence":["specified","planned","implemented","verified"]}
required-artifacts: ["spec","implementation-plan","verification-report"]
child-work-refs: []
budget-limits: null
---

# LLM evaluation methodology for domain and architecture modeling

## Description

Create an offline renovatio-evals package with versioned COBOL fixtures, goldens, rubrics, schema/reference/hallucination checks, comparable prompt/model reports, and Agora-linked regression gates for issue #173.

## Acceptance criteria

- [ ] **schema-validity:** Evaluation cases and sample outputs validate against versioned schemas and all IR references resolve.; stages: specified, planned, implemented, verified
- [ ] **critical-regressions:** Critical fixtures have baseline goldens and regression gates fail on invalid, hallucinated, or unsafe outputs.; stages: specified, planned, implemented, verified
- [ ] **comparable-reports:** Every prompt/model/version run emits a deterministic report comparable with a baseline.; stages: specified, planned, implemented, verified
- [ ] **human-review-boundary:** LLM suggestions are represented as reviewable decisions and never promoted as final code.; stages: specified, planned, implemented, verified
- [ ] **metrics:** Reports include acceptance, fallback, cost, latency, and cache-hit metrics fields.; stages: specified, planned, implemented, verified
- [ ] **agora-evidence:** Specification, implementation plan, verification report, and test evidence are registered in Agora.; stages: specified, planned, implemented, verified

## Required artifacts

- spec
- implementation-plan
- verification-report
