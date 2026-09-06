---
schema: "agora/work/v1"
id: "issue-177-theia-ide-shell"
swarm: "renovatio-workbench"
title: "Issue #177 \u00b7 Theia 1 \u00b7 Shell IDE y navegaci\u00f3n de proyecto"
state: "verifying"
revision: 1
operational-status: "active"
status-reason: "Explicit user-authorized exception: begin issue #177 while #176 remains in verifying. This exception is limited to development work; browser/macOS evidence and security findings from #176 remain open and no production or release approval is implied."
status-by: "project:agent"
status-at: "2026-09-06T02:24:08.501510Z"
acceptance-criteria: {"project-navigation":"A user can open a project and navigate COBOL sources, copybooks, JCL, models, runs and evidence without leaving Theia.","activity-shell":"The Activity Bar exposes Project, Analysis, Architecture, AI and Equivalence areas with tabs, resizable panels and a bottom panel.","command-access":"Initial commands are available through the command palette, menus and documented keyboard bindings.","dashboard-continuity":"The existing React administrative dashboard remains accessible through an embedded or linked view.","state-persistence":"Layout and the last user/project selection survive reloads with explicit loading, error, permission and selected-project states.","accessibility":"Core navigation is keyboard-operable and exposes appropriate accessible names and ARIA semantics.","wizard-decoupling":"The Theia shell has no direct dependency on internal components of the existing wizard.","verification-evidence":"Extension tests, navigation E2E smoke tests, accessibility checks and command documentation are recorded as successful evidence."}
satisfied-criteria: []
criterion-statuses: {"project-navigation":["specified","planned","implemented","verified"],"activity-shell":["specified","planned","implemented","verified"],"command-access":["specified","planned","implemented","verified"],"dashboard-continuity":["specified","planned","implemented","verified"],"state-persistence":["specified","planned","implemented","verified"],"accessibility":["specified","planned","implemented","verified"],"wizard-decoupling":["specified","planned","implemented","verified"],"verification-evidence":["specified","planned","implemented"]}
required-artifacts: ["spec","implementation-plan","extension-package","command-reference","accessibility-report","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Issue #177 · Theia 1 · Shell IDE y navegación de proyecto

## Description

Progressively replace the wizard with a navigable Theia IDE shell without removing the current dashboard. Provide Renovatio activities, project exploration, tabs and panels, command palette, persisted layout and accessible navigation while remaining decoupled from wizard internals.

## Acceptance criteria

- [ ] **project-navigation:** A user can open a project and navigate COBOL sources, copybooks, JCL, models, runs and evidence without leaving Theia.; stages: specified, planned, implemented, verified
- [ ] **activity-shell:** The Activity Bar exposes Project, Analysis, Architecture, AI and Equivalence areas with tabs, resizable panels and a bottom panel.; stages: specified, planned, implemented, verified
- [ ] **command-access:** Initial commands are available through the command palette, menus and documented keyboard bindings.; stages: specified, planned, implemented, verified
- [ ] **dashboard-continuity:** The existing React administrative dashboard remains accessible through an embedded or linked view.; stages: specified, planned, implemented, verified
- [ ] **state-persistence:** Layout and the last user/project selection survive reloads with explicit loading, error, permission and selected-project states.; stages: specified, planned, implemented, verified
- [ ] **accessibility:** Core navigation is keyboard-operable and exposes appropriate accessible names and ARIA semantics.; stages: specified, planned, implemented, verified
- [ ] **wizard-decoupling:** The Theia shell has no direct dependency on internal components of the existing wizard.; stages: specified, planned, implemented, verified
- [ ] **verification-evidence:** Extension tests, navigation E2E smoke tests, accessibility checks and command documentation are recorded as successful evidence.; stages: specified, planned, implemented

## Required artifacts

- spec
- implementation-plan
- extension-package
- command-reference
- accessibility-report
- verification-report
- review-report
