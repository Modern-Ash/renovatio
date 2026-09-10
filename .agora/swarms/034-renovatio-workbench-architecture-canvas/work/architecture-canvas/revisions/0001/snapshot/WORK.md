---
schema: "agora/work/v1"
id: "architecture-canvas"
swarm: "renovatio-workbench-architecture-canvas"
title: "Theia 4 Architecture Canvas and configurable profiles"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"domain-model-immutable":"Changing architecture profile selection or naming never mutates the persisted DomainModel.","mvc-defaults":"MVC defaults produce model, service, and controller conventions for Java projects.","custom-naming-shadow":"Custom package, suffix, adapter, model, service, controller, and class naming recomputes the shadow/preview before generation.","illegal-dependencies-visible":"Allowed and forbidden dependency rules are evaluated live and illegal dependencies are visible before generation.","profile-versioned-hash":"Saved architecture profiles are versioned, comparable/restorable, and reproducible by hash."}
satisfied-criteria: ["domain-model-immutable","mvc-defaults","custom-naming-shadow","illegal-dependencies-visible","profile-versioned-hash"]
criterion-statuses: {"domain-model-immutable":["specified","planned","implemented","verified","accepted"],"mvc-defaults":["specified","planned","implemented","verified","accepted"],"custom-naming-shadow":["specified","planned","implemented","verified","accepted"],"illegal-dependencies-visible":["specified","planned","implemented","verified","accepted"],"profile-versioned-hash":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","implementation","verification"]
child-work-refs: []
budget-limits: null
---

# Theia 4 Architecture Canvas and configurable profiles

## Description

GitHub #180: make the target architecture selectable and editable, including Java/MVC conventions, saveable profile versions, editable package/class naming, dependency rules, artifact/package manifest preview, live validation, impact comparison, and restoration.

## Acceptance criteria

- [x] **domain-model-immutable:** Changing architecture profile selection or naming never mutates the persisted DomainModel.; stages: specified, planned, implemented, verified, accepted
- [x] **mvc-defaults:** MVC defaults produce model, service, and controller conventions for Java projects.; stages: specified, planned, implemented, verified, accepted
- [x] **custom-naming-shadow:** Custom package, suffix, adapter, model, service, controller, and class naming recomputes the shadow/preview before generation.; stages: specified, planned, implemented, verified, accepted
- [x] **illegal-dependencies-visible:** Allowed and forbidden dependency rules are evaluated live and illegal dependencies are visible before generation.; stages: specified, planned, implemented, verified, accepted
- [x] **profile-versioned-hash:** Saved architecture profiles are versioned, comparable/restorable, and reproducible by hash.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- implementation
- verification
