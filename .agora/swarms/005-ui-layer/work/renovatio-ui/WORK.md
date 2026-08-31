---
schema: "agora/work/v1"
id: "renovatio-ui"
swarm: "ui-layer"
title: "renovatio-ui: SPA wizard + dashboard (issue #130 phase 3)"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"wizard-flow":"Wizard guides analyst through: folder selection -> analyze (SSE progress) -> metrics+complexity display -> plan configuration -> dry-run apply -> side-by-side diffs -> action item review (accept/reject) -> real apply or export","dashboard-view":"Dashboard shows: metric cards (LOC, cyclomatic complexity, copybooks, complex procedures), gate status per program, action items by severity, job timeline","export-capabilities":"Export HTML/PDF reports from dashboard; export action items with review status","no-regression":"Build succeeds; renovatio-api serves static assets; existing API endpoints unchanged","tested":"Component tests for wizard steps; dashboard rendering tests; API integration tests"}
satisfied-criteria: ["wizard-flow","dashboard-view","export-capabilities","no-regression","tested"]
criterion-statuses: {"wizard-flow":["specified","planned","implemented","verified","accepted"],"dashboard-view":["specified","planned","implemented","verified","accepted"],"export-capabilities":["specified","planned","implemented","verified","accepted"],"no-regression":["specified","planned","implemented","verified","accepted"],"tested":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","test-report"]
child-work-refs: []
budget-limits: null
---

# renovatio-ui: SPA wizard + dashboard (issue #130 phase 3)

## Description

Vite + React SPA for Renovatio migration management. Wizard for analysts: select folder -> analyze (SSE progress) -> metrics+complexity -> configure plan -> apply dry-run -> side-by-side diffs -> review action items -> apply real/export. Dashboard for leads: metric cards, gate status per program, action items by severity, job timeline, HTML/PDF export.

## Acceptance criteria

- [x] **wizard-flow:** Wizard guides analyst through: folder selection -> analyze (SSE progress) -> metrics+complexity display -> plan configuration -> dry-run apply -> side-by-side diffs -> action item review (accept/reject) -> real apply or export; stages: specified, planned, implemented, verified, accepted
- [x] **dashboard-view:** Dashboard shows: metric cards (LOC, cyclomatic complexity, copybooks, complex procedures), gate status per program, action items by severity, job timeline; stages: specified, planned, implemented, verified, accepted
- [x] **export-capabilities:** Export HTML/PDF reports from dashboard; export action items with review status; stages: specified, planned, implemented, verified, accepted
- [x] **no-regression:** Build succeeds; renovatio-api serves static assets; existing API endpoints unchanged; stages: specified, planned, implemented, verified, accepted
- [x] **tested:** Component tests for wizard steps; dashboard rendering tests; API integration tests; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- test-report
