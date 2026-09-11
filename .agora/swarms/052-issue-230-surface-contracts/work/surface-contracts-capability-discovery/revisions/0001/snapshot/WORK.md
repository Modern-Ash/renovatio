---
schema: "agora/work/v1"
id: "surface-contracts-capability-discovery"
swarm: "issue-230-surface-contracts"
title: "AC-09: Unify API, CLI, MCP and Workbench through capabilities"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"capability-schema":"A versioned contract declares sources, targets, preview/apply, LLM, persistence, equivalence and maturity stable/beta/experimental/planned.","common-semantics":"Equivalent API, CLI and MCP operations use the same IDs, states, errors, hashes, manifests and authorization rules.","no-duplication":"Controllers, commands and MCP tools do not project architecture or generate artifacts directly.","api-contract":"OpenAPI and MCP/CLI schemas are generated or validated against application contracts with versioned compatibility.","workbench-client":"Workbench consumes documented public API and does not access wizard internals or Spring services directly.","legacy-ui":"renovatio-ui has an explicit converge, temporary-maintain or retire decision with date and critical-route coverage.","contract-suite":"Parameterized tests execute the same supported success and error scenarios across each supported surface.","truthful-docs":"README and capability endpoint do not advertise targets or features that fail their end-to-end path."}
satisfied-criteria: ["capability-schema","common-semantics","no-duplication","api-contract","workbench-client","legacy-ui","contract-suite","truthful-docs"]
criterion-statuses: {"capability-schema":["specified","planned","implemented","verified","accepted"],"common-semantics":["specified","planned","implemented","verified","accepted"],"no-duplication":["specified","planned","implemented","verified","accepted"],"api-contract":["specified","planned","implemented","verified","accepted"],"workbench-client":["specified","planned","implemented","verified","accepted"],"legacy-ui":["specified","planned","implemented","verified","accepted"],"contract-suite":["specified","planned","implemented","verified","accepted"],"truthful-docs":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","capability-contract","api-contract","compatibility-report","test-report"]
child-work-refs: []
budget-limits: null
---

# AC-09: Unify API, CLI, MCP and Workbench through capabilities

## Description

Issue #230. Make API, CLI, MCP and Workbench thin adapters over shared application contracts, with versioned capability discovery, common semantics, contract tests, truthful docs, and explicit legacy UI disposition.

## Acceptance criteria

- [x] **capability-schema:** A versioned contract declares sources, targets, preview/apply, LLM, persistence, equivalence and maturity stable/beta/experimental/planned.; stages: specified, planned, implemented, verified, accepted
- [x] **common-semantics:** Equivalent API, CLI and MCP operations use the same IDs, states, errors, hashes, manifests and authorization rules.; stages: specified, planned, implemented, verified, accepted
- [x] **no-duplication:** Controllers, commands and MCP tools do not project architecture or generate artifacts directly.; stages: specified, planned, implemented, verified, accepted
- [x] **api-contract:** OpenAPI and MCP/CLI schemas are generated or validated against application contracts with versioned compatibility.; stages: specified, planned, implemented, verified, accepted
- [x] **workbench-client:** Workbench consumes documented public API and does not access wizard internals or Spring services directly.; stages: specified, planned, implemented, verified, accepted
- [x] **legacy-ui:** renovatio-ui has an explicit converge, temporary-maintain or retire decision with date and critical-route coverage.; stages: specified, planned, implemented, verified, accepted
- [x] **contract-suite:** Parameterized tests execute the same supported success and error scenarios across each supported surface.; stages: specified, planned, implemented, verified, accepted
- [x] **truthful-docs:** README and capability endpoint do not advertise targets or features that fail their end-to-end path.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- capability-contract
- api-contract
- compatibility-report
- test-report
