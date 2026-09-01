---
schema: "agora/work/v1"
id: "f1-decision-layer"
swarm: "decision-engine-f1"
title: "F1 \u00b7 Migration profile and decision layer (issue #146)"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"profile-contract":"MigrationProfile v1 has a versioned JSON schema, YAML/object round-trip, validation, extensions, legacy-field migration, and effective precedence defaults < profile < confirmed or overridden decisions","decision-contract":"DecisionPoint, stable semantic-coordinate ids, persistence store, resolver precedence, statuses, option validation, and threshold bulk-confirm are implemented and tested","llm-suggestions":"DecisionSuggestionService uses the existing PromptRuntime validation cache and deterministic fallback, never blocks the wizard, and exposes failure telemetry without allowing LLM output to bypass options","api-contract":"The five profile and decision endpoints implement the issue #146 request, filtering, validation, 400/422 semantics, and contract tests","ui-workflow":"The Target and Decisions wizard steps, clients, filters, rationale, confirmation, editing, and bulk-confirm interactions are implemented with component tests","compatibility":"Default profile generation is byte-identical to the current Java output and the characterization harness, mvn clean install, and MCP regression checks pass","scope-boundaries":"Node and Python emitters, real architecture transformation, data-access classification, configurable rules, and cross-project reusable profiles remain excluded"}
satisfied-criteria: []
criterion-statuses: {"profile-contract":["specified"],"decision-contract":["specified"],"llm-suggestions":["specified"],"api-contract":["specified"],"ui-workflow":["specified"],"compatibility":["specified"],"scope-boundaries":["specified"]}
required-artifacts: ["spec"]
child-work-refs: []
budget-limits: null
---

# F1 · Migration profile and decision layer (issue #146)

## Description

Implement the F1 decision layer from issue #146 under spec-driven TDD, using F0 cartography as the authoritative input. Add versioned profiles, stable decisions, bounded LLM suggestions, APIs, and UI workflow while preserving current default generation and the explicit YAGNI boundaries.

## Acceptance criteria

- [ ] **profile-contract:** MigrationProfile v1 has a versioned JSON schema, YAML/object round-trip, validation, extensions, legacy-field migration, and effective precedence defaults < profile < confirmed or overridden decisions; stages: specified
- [ ] **decision-contract:** DecisionPoint, stable semantic-coordinate ids, persistence store, resolver precedence, statuses, option validation, and threshold bulk-confirm are implemented and tested; stages: specified
- [ ] **llm-suggestions:** DecisionSuggestionService uses the existing PromptRuntime validation cache and deterministic fallback, never blocks the wizard, and exposes failure telemetry without allowing LLM output to bypass options; stages: specified
- [ ] **api-contract:** The five profile and decision endpoints implement the issue #146 request, filtering, validation, 400/422 semantics, and contract tests; stages: specified
- [ ] **ui-workflow:** The Target and Decisions wizard steps, clients, filters, rationale, confirmation, editing, and bulk-confirm interactions are implemented with component tests; stages: specified
- [ ] **compatibility:** Default profile generation is byte-identical to the current Java output and the characterization harness, mvn clean install, and MCP regression checks pass; stages: specified
- [ ] **scope-boundaries:** Node and Python emitters, real architecture transformation, data-access classification, configurable rules, and cross-project reusable profiles remain excluded; stages: specified

## Required artifacts

- spec
