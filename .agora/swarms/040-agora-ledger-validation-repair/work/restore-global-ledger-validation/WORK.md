---
schema: "agora/work/v1"
id: "restore-global-ledger-validation"
swarm: "agora-ledger-validation-repair"
title: "Restore global Agora ledger validation"
state: "verifying"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"validate-clean":"agora validate completes with no errors and no warnings.","stale-disposition":"Stale clarification and consistency warnings are refreshed or documented through governed disposition.","history-preserved":"Historical evidence remains traceable through governance records and repair documentation.","terminal-work-preserved":"Previously completed work and swarm states remain terminal.","governance-only":"Changes are limited to governance records and evidence repair.","final-report":"Final verification report records the validation command, result, and retained warning state."}
satisfied-criteria: []
criterion-statuses: {"validate-clean":["specified","planned","implemented","verified"],"stale-disposition":["specified","planned","implemented","verified"],"history-preserved":["specified","planned","implemented","verified"],"terminal-work-preserved":["specified","planned","implemented","verified"],"governance-only":["specified","planned","implemented","verified"],"final-report":["specified","planned","implemented","verified"]}
required-artifacts: ["repair-report"]
child-work-refs: []
budget-limits: null
---

# Restore global Agora ledger validation

## Description

Repair repository-wide Agora governance debt tracked by GitHub #188 while preserving completed work history and limiting changes to governance records.

## Acceptance criteria

- [ ] **validate-clean:** agora validate completes with no errors and no warnings.; stages: specified, planned, implemented, verified
- [ ] **stale-disposition:** Stale clarification and consistency warnings are refreshed or documented through governed disposition.; stages: specified, planned, implemented, verified
- [ ] **history-preserved:** Historical evidence remains traceable through governance records and repair documentation.; stages: specified, planned, implemented, verified
- [ ] **terminal-work-preserved:** Previously completed work and swarm states remain terminal.; stages: specified, planned, implemented, verified
- [ ] **governance-only:** Changes are limited to governance records and evidence repair.; stages: specified, planned, implemented, verified
- [ ] **final-report:** Final verification report records the validation command, result, and retained warning state.; stages: specified, planned, implemented, verified

## Required artifacts

- repair-report
