---
schema: "agora/work/v1"
id: "issue-173-llm-eval-methodology"
swarm: "llm-eval-methodology"
title: "LLM evaluation methodology for domain and architecture modeling"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"schema-validity":"Evaluation cases and sample outputs validate against versioned schemas and all IR references resolve.","critical-regressions":"Critical fixtures have baseline goldens and regression gates fail on invalid, hallucinated, or unsafe outputs.","comparable-reports":"Every prompt/model/version run emits a deterministic report comparable with a baseline.","human-review-boundary":"LLM suggestions are represented as reviewable decisions and never promoted as final code.","metrics":"Reports include acceptance, fallback, cost, latency, and cache-hit metrics fields.","agora-evidence":"Specification, implementation plan, verification report, and test evidence are registered in Agora."}
satisfied-criteria: ["schema-validity","critical-regressions","comparable-reports","human-review-boundary","metrics","agora-evidence"]
criterion-statuses: {"schema-validity":["specified","planned","implemented","verified","accepted"],"critical-regressions":["specified","planned","implemented","verified","accepted"],"comparable-reports":["specified","planned","implemented","verified","accepted"],"human-review-boundary":["specified","planned","implemented","verified","accepted"],"metrics":["specified","planned","implemented","verified","accepted"],"agora-evidence":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","verification-report"]
child-work-refs: []
budget-limits: null
---

# LLM evaluation methodology for domain and architecture modeling

## Description

Create an offline renovatio-evals package with versioned COBOL fixtures, goldens, rubrics, schema/reference/hallucination checks, comparable prompt/model reports, and Agora-linked regression gates for issue #173.

## Acceptance criteria

- [x] **schema-validity:** Evaluation cases and sample outputs validate against versioned schemas and all IR references resolve.; stages: specified, planned, implemented, verified, accepted
- [x] **critical-regressions:** Critical fixtures have baseline goldens and regression gates fail on invalid, hallucinated, or unsafe outputs.; stages: specified, planned, implemented, verified, accepted
- [x] **comparable-reports:** Every prompt/model/version run emits a deterministic report comparable with a baseline.; stages: specified, planned, implemented, verified, accepted
- [x] **human-review-boundary:** LLM suggestions are represented as reviewable decisions and never promoted as final code.; stages: specified, planned, implemented, verified, accepted
- [x] **metrics:** Reports include acceptance, fallback, cost, latency, and cache-hit metrics fields.; stages: specified, planned, implemented, verified, accepted
- [x] **agora-evidence:** Specification, implementation plan, verification report, and test evidence are registered in Agora.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- verification-report
