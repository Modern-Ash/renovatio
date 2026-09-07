---
schema: "agora/work/v1"
id: "local-h2-to-sqlite"
swarm: "renovatio-sqlite-migration"
title: "Migraci\u00f3n local de H2 a SQLite"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"source-preservation":"Existing H2 files remain unchanged and are retained as rollback artifacts.","sqlite-configuration":"Runtime configuration uses SQLite and no H2 console or datasource remains active.","data-copy":"SQLite contains the schema, primary keys and copied rows from the source database.","validation":"Per-table row counts are validated before runtime cutover."}
satisfied-criteria: ["source-preservation","sqlite-configuration","data-copy","validation"]
criterion-statuses: {"source-preservation":["specified","planned","implemented","verified","accepted"],"sqlite-configuration":["specified","planned","implemented","verified","accepted"],"data-copy":["specified","planned","implemented","verified","accepted"],"validation":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","migration-plan","verification-report"]
child-work-refs: []
budget-limits: null
---

# Migración local de H2 a SQLite

## Description

Copy local H2 development data into SQLite without modifying the source files.

## Acceptance criteria

- [x] **source-preservation:** Existing H2 files remain unchanged and are retained as rollback artifacts.; stages: specified, planned, implemented, verified, accepted
- [x] **sqlite-configuration:** Runtime configuration uses SQLite and no H2 console or datasource remains active.; stages: specified, planned, implemented, verified, accepted
- [x] **data-copy:** SQLite contains the schema, primary keys and copied rows from the source database.; stages: specified, planned, implemented, verified, accepted
- [x] **validation:** Per-table row counts are validated before runtime cutover.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- migration-plan
- verification-report
