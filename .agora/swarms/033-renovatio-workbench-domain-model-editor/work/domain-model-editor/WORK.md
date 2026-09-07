---
schema: "agora/work/v1"
id: "domain-model-editor"
swarm: "renovatio-workbench-domain-model-editor"
title: "Theia 3 \u00b7 Business DomainModel view and editor"
state: "verifying"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"element-views":"Entities, value objects, aggregates, services, events and rules are each viewable with their attributes.","property-editor":"Properties, relationships, cardinality and names are editable through a structured editor.","provenance-inspector":"Every element exposes originating paragraphs, copybooks, hashes and confidence.","bidirectional-navigation":"Navigation resolves COBOL to DomainModel element and DomainModel element back to COBOL origin.","versioning-compare":"The model can be saved as versions and two versions compared as a structured diff.","ai-suggestion-triage":"AI suggestions can be accepted, edited or rejected and the outcome is recorded.","validation-guards":"Invalid relationships and missing-evidence elements are flagged and an invalid relationship cannot be saved.","preview-live-neutral":"Editing updates the preview without regenerating files and the saved model stays neutral w.r.t. MVC/Hexagonal.","verification-evidence":"Model, persistence, traceability unit tests and an edit E2E smoke are recorded."}
satisfied-criteria: []
criterion-statuses: {"element-views":["specified","planned","implemented","verified"],"property-editor":["specified","planned","implemented","verified"],"provenance-inspector":["specified","planned","implemented","verified"],"bidirectional-navigation":["specified","planned","implemented","verified"],"versioning-compare":["specified","planned","implemented","verified"],"ai-suggestion-triage":["specified","planned","implemented","verified"],"validation-guards":["specified","planned","implemented","verified"],"preview-live-neutral":["specified","planned","implemented","verified"],"verification-evidence":["specified","planned","implemented","verified"]}
required-artifacts: ["spec","domain-model-editor-contract","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Theia 3 · Business DomainModel view and editor

## Description

Governed Theia view+editor for the abstract business DomainModel with provenance, bidirectional COBOL navigation, versioning/compare, AI-suggestion triage, and validation. Read/write to DomainModel; no MVC/Hexagonal projection; no file regeneration on edit.

## Acceptance criteria

- [ ] **element-views:** Entities, value objects, aggregates, services, events and rules are each viewable with their attributes.; stages: specified, planned, implemented, verified
- [ ] **property-editor:** Properties, relationships, cardinality and names are editable through a structured editor.; stages: specified, planned, implemented, verified
- [ ] **provenance-inspector:** Every element exposes originating paragraphs, copybooks, hashes and confidence.; stages: specified, planned, implemented, verified
- [ ] **bidirectional-navigation:** Navigation resolves COBOL to DomainModel element and DomainModel element back to COBOL origin.; stages: specified, planned, implemented, verified
- [ ] **versioning-compare:** The model can be saved as versions and two versions compared as a structured diff.; stages: specified, planned, implemented, verified
- [ ] **ai-suggestion-triage:** AI suggestions can be accepted, edited or rejected and the outcome is recorded.; stages: specified, planned, implemented, verified
- [ ] **validation-guards:** Invalid relationships and missing-evidence elements are flagged and an invalid relationship cannot be saved.; stages: specified, planned, implemented, verified
- [ ] **preview-live-neutral:** Editing updates the preview without regenerating files and the saved model stays neutral w.r.t. MVC/Hexagonal.; stages: specified, planned, implemented, verified
- [ ] **verification-evidence:** Model, persistence, traceability unit tests and an edit E2E smoke are recorded.; stages: specified, planned, implemented, verified

## Required artifacts

- spec
- domain-model-editor-contract
- implementation-plan
- verification-report
- review-report
