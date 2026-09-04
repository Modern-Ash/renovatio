---
schema: "agora/work/v1"
id: "f8-reusable-profiles-policies"
swarm: "decision-engine-f8"
title: "F8 \u00b7 Reusable migration profiles and decision policies (issue #154)"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"template-reuse":"Saving a named versioned profile template from project A and applying that exact version to project B populates B with equivalent profile values through domain, CLI, and API contracts; filesystem storage is deterministic and local-first.","policy-reuse":"Exporting confirmed decisions from A and applying its versioned policy catalog to equivalent semantic decision points in B confirms high-confidence matches with source POLICY while new or sub-threshold points remain reviewable and the result reports auto-confirmed and suggested counts.","precedence-overrides":"The effective configuration obeys defaults < template < policy-catalog < project profile < project decisions; a local inherited-value override preserves template linkage and profile diff identifies the deviation.","version-safety":"A project binds explicit template and policy-catalog versions; catalog v1 and v2 may coexist and resolution uses the bound version rather than latest, with stale policy provenance visible and all inherited results locally overridable.","ui-management":"Project creation can select a template; decision review identifies policy provenance and permits local override; management surfaces list, create, version, and show project usage of profile templates and policy catalogs.","compatibility-quality":"Existing default migration behavior remains compatible, security-sensitive paths reject traversal, focused module tests and repository build pass, and the characterization guardrail runs because decision resolution affects generation."}
satisfied-criteria: ["template-reuse","policy-reuse","precedence-overrides","version-safety","ui-management","compatibility-quality"]
criterion-statuses: {"template-reuse":["specified","planned","implemented","verified","accepted"],"policy-reuse":["specified","planned","implemented","verified","accepted"],"precedence-overrides":["specified","planned","implemented","verified","accepted"],"version-safety":["specified","planned","implemented","verified","accepted"],"ui-management":["specified","planned","implemented","verified","accepted"],"compatibility-quality":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","verification-report","consistency-report","review-report"]
child-work-refs: []
budget-limits: null
---

# F8 · Reusable migration profiles and decision policies (issue #154)

## Description

One governed spec→plan→implementation→verification cycle for GitHub issue #154. Add reusable versioned MigrationProfileTemplate storage and save/apply/diff flows; reusable decision-policy catalogs with semantic matching, policy provenance, confidence thresholds, version binding, explicit precedence, local overrides, CLI/API/UI surfaces, usage reporting, and tests. Depends on F1. Excludes remote marketplace, organization RBAC, and automatic propagation when templates change.

## Acceptance criteria

- [x] **template-reuse:** Saving a named versioned profile template from project A and applying that exact version to project B populates B with equivalent profile values through domain, CLI, and API contracts; filesystem storage is deterministic and local-first.; stages: specified, planned, implemented, verified, accepted
- [x] **policy-reuse:** Exporting confirmed decisions from A and applying its versioned policy catalog to equivalent semantic decision points in B confirms high-confidence matches with source POLICY while new or sub-threshold points remain reviewable and the result reports auto-confirmed and suggested counts.; stages: specified, planned, implemented, verified, accepted
- [x] **precedence-overrides:** The effective configuration obeys defaults < template < policy-catalog < project profile < project decisions; a local inherited-value override preserves template linkage and profile diff identifies the deviation.; stages: specified, planned, implemented, verified, accepted
- [x] **version-safety:** A project binds explicit template and policy-catalog versions; catalog v1 and v2 may coexist and resolution uses the bound version rather than latest, with stale policy provenance visible and all inherited results locally overridable.; stages: specified, planned, implemented, verified, accepted
- [x] **ui-management:** Project creation can select a template; decision review identifies policy provenance and permits local override; management surfaces list, create, version, and show project usage of profile templates and policy catalogs.; stages: specified, planned, implemented, verified, accepted
- [x] **compatibility-quality:** Existing default migration behavior remains compatible, security-sensitive paths reject traversal, focused module tests and repository build pass, and the characterization guardrail runs because decision resolution affects generation.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- verification-report
- consistency-report
- review-report
