---
schema: "agora/swarm/v1"
id: "renovatio-workbench-source-explorer"
method: "spec-driven"
status: "running"
branch: "agora/renovatio-workbench"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm renovatio-workbench-source-explorer

## Objective

Deliver the next Renovatio Theia increment (GitHub issue #178): a governed read-only Source Explorer that turns legacy COBOL, copybooks and JCL into a navigable workspace with a program/section/paragraph/copybook/JCL/dataset tree, an outline/symbol view for divisions, paragraphs, PERFORM, CALL, SQL and CICS, symbol/reference/text search, per-file hash/path/encoding/analysis-status metadata, and selection linked to the semantic IR and diagnostics — without changing backend analysis flows or implementing deferred identity/login.

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
