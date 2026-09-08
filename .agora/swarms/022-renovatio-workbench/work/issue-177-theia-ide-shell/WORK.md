---
schema: "agora/work/v1"
id: "issue-177-theia-ide-shell"
swarm: "renovatio-workbench"
title: "Issue #177 \u00b7 Theia 1 \u00b7 Shell IDE y navegaci\u00f3n de proyecto"
state: "drafting"
revision: 1
operational-status: "cancelled"
status-reason: "Superseded during reconciliation for GitHub issue #202: GitHub issue #177 closed by PR #186, while later completed issues #178-#185 and #200 carry the implemented shell, navigation, and e2e evidence."
status-by: "project:owner"
status-at: "2026-09-08T01:13:40.174525Z"
acceptance-criteria: {"project-navigation":"A user can open a project and navigate COBOL sources, copybooks, JCL, models, runs and evidence without leaving Theia.","activity-shell":"The Activity Bar exposes Project, Analysis, Architecture, AI and Equivalence areas with tabs, resizable panels and a bottom panel.","command-access":"Initial commands are available through the command palette, menus and documented keyboard bindings.","dashboard-continuity":"The existing React administrative dashboard remains accessible through an embedded or linked view.","state-persistence":"Layout and the last user/project selection survive reloads with explicit loading, error, permission and selected-project states.","accessibility":"Core navigation is keyboard-operable and exposes appropriate accessible names and ARIA semantics.","wizard-decoupling":"The Theia shell has no direct dependency on internal components of the existing wizard.","verification-evidence":"Extension tests, navigation E2E smoke tests, accessibility checks and command documentation are recorded as successful evidence."}
satisfied-criteria: []
criterion-statuses: {"project-navigation":[],"activity-shell":[],"command-access":[],"dashboard-continuity":[],"state-persistence":[],"accessibility":[],"wizard-decoupling":[],"verification-evidence":[]}
required-artifacts: ["spec","implementation-plan","extension-package","command-reference","accessibility-report","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Issue #177 · Theia 1 · Shell IDE y navegación de proyecto

## Description

Progressively replace the wizard with a navigable Theia IDE shell without removing the current dashboard. Provide Renovatio activities, project exploration, tabs and panels, command palette, persisted layout and accessible navigation while remaining decoupled from wizard internals.

## Acceptance criteria

- [ ] **project-navigation:** A user can open a project and navigate COBOL sources, copybooks, JCL, models, runs and evidence without leaving Theia.; stages: none
- [ ] **activity-shell:** The Activity Bar exposes Project, Analysis, Architecture, AI and Equivalence areas with tabs, resizable panels and a bottom panel.; stages: none
- [ ] **command-access:** Initial commands are available through the command palette, menus and documented keyboard bindings.; stages: none
- [ ] **dashboard-continuity:** The existing React administrative dashboard remains accessible through an embedded or linked view.; stages: none
- [ ] **state-persistence:** Layout and the last user/project selection survive reloads with explicit loading, error, permission and selected-project states.; stages: none
- [ ] **accessibility:** Core navigation is keyboard-operable and exposes appropriate accessible names and ARIA semantics.; stages: none
- [ ] **wizard-decoupling:** The Theia shell has no direct dependency on internal components of the existing wizard.; stages: none
- [ ] **verification-evidence:** Extension tests, navigation E2E smoke tests, accessibility checks and command documentation are recorded as successful evidence.; stages: none

## Required artifacts

- spec
- implementation-plan
- extension-package
- command-reference
- accessibility-report
- verification-report
- review-report
