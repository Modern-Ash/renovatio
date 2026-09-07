---
schema: "agora/swarm/v1"
id: "renovatio-workbench-domain-model-editor"
method: "spec-driven"
status: "ready"
branch: "agora/renovatio-workbench"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm renovatio-workbench-domain-model-editor

## Objective

Deliver the next Renovatio Theia increment (GitHub issue #179): a governed view and editor for the abstract business DomainModel — entity/value-object/aggregate/service/event/rule views; property, relationship, cardinality and naming editor; provenance inspector (paragraphs, copybooks, hashes, confidence); bidirectional COBOL <-> DomainModel navigation; model versioning, comparison and save; accept/edit/reject actions for AI suggestions; broken-reference and missing-evidence validation. Editing updates the preview without regenerating files; invalid relationships cannot be saved; every element shows provenance; two versions can be compared; the model stays neutral w.r.t. MVC/Hexagonal. Depends on #178 (Source Explorer, completed).

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
