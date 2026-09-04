---
schema: "agora/work/v1"
id: "epic-cli-gaps"
swarm: "decision-engine-epic-gaps"
title: "Complete epic profile and generation CLI gaps"
state: "verifying"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"profile-init":"profile init creates a valid versioned project profile without overwriting an existing profile unless explicitly forced.","explicit-generation":"generate accepts an explicit JSON or YAML profile, validates and persists it as project state, routes STUBS generation through LanguageProviderRegistry, supports output selection, and returns useful human/JSON results.","target-availability":"The packaged CLI can generate with the Java compatibility path and the registered Node emitter; unsupported targets fail closed with structured availability details.","overlay-rebinding":"Applying template A and later template B preserves the sparse project overlay, so B supplies inherited values while explicit local overrides continue to win.","cli-contract":"Root help, profile help, README usage/state documentation, and failure exit codes reflect the new commands and overlay semantics.","regression-quality":"Focused tests, full Maven reactor, characterization guardrail, UI tests/build, and whitespace checks pass."}
satisfied-criteria: []
criterion-statuses: {"profile-init":["specified","planned","implemented","verified"],"explicit-generation":["specified","planned","implemented","verified"],"target-availability":["specified","planned","implemented","verified"],"overlay-rebinding":["specified","planned","implemented","verified"],"cli-contract":["specified","planned","implemented","verified"],"regression-quality":["specified","planned","implemented","verified"]}
required-artifacts: ["spec","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Complete epic profile and generation CLI gaps

## Description

Close the explicit profile init and generate --profile gaps from epic #152, expose target generation through the provider registry, ensure the Node emitter is available to the CLI, and resolve the PR review finding about template rebindings.

## Acceptance criteria

- [ ] **profile-init:** profile init creates a valid versioned project profile without overwriting an existing profile unless explicitly forced.; stages: specified, planned, implemented, verified
- [ ] **explicit-generation:** generate accepts an explicit JSON or YAML profile, validates and persists it as project state, routes STUBS generation through LanguageProviderRegistry, supports output selection, and returns useful human/JSON results.; stages: specified, planned, implemented, verified
- [ ] **target-availability:** The packaged CLI can generate with the Java compatibility path and the registered Node emitter; unsupported targets fail closed with structured availability details.; stages: specified, planned, implemented, verified
- [ ] **overlay-rebinding:** Applying template A and later template B preserves the sparse project overlay, so B supplies inherited values while explicit local overrides continue to win.; stages: specified, planned, implemented, verified
- [ ] **cli-contract:** Root help, profile help, README usage/state documentation, and failure exit codes reflect the new commands and overlay semantics.; stages: specified, planned, implemented, verified
- [ ] **regression-quality:** Focused tests, full Maven reactor, characterization guardrail, UI tests/build, and whitespace checks pass.; stages: specified, planned, implemented, verified

## Required artifacts

- spec
- implementation-plan
- verification-report
- review-report
