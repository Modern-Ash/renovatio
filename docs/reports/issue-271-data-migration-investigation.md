# Issue 271 - Data Migration Investigation

Date: 2026-09-12

## Finding

Renovatio already has fixture-level representations of legacy data, but not a production data extraction adapter for VSAM/DB2 content migration.

Reusable pieces found:

- `EquivalenceFixture.inputs.sequentialFiles` in the Workbench UI contract captures sequential file inputs with `ddName`, `contentHash` and `preview`.
- `EquivalenceFixture.inputs.db2Responses` captures DB2 statement responses with `statementId`, `sqlState` and sample rows.
- `renovatio-provider-cobol/src/test/resources/corpus/carddemo` contains JCL and COBOL examples referencing VSAM KSDS/ESDS/RRDS datasets and DB2 programs.
- `WorkbenchChangeSetService` already provides the governed create/review/approve/apply/rollback surface needed for generated migration scripts.

Gap:

- No backend service currently reads production VSAM clusters or DB2 tables and streams real records into a migration plan.
- Existing fixture data is suitable for dry-run previews and analyst validation, but not for production execution.

## Implemented First Cut

The DomainModel now carries the source-to-target metadata needed by the issue:

- `DomainNode.tableName`
- `DomainNode.sourceDataset`
- `DomainProperty.isKey`
- `DomainProperty.columnName`
- `DomainProperty.sourceColumn`
- `DomainProperty.sourceDataset`
- `DomainRelation.foreignKey.property`
- `DomainRelation.foreignKey.referencesProperty`

The Workbench Domain editor exposes these fields alongside the existing model editor. The diagram mapper passes table/source metadata to the node renderer, so the canvas can surface data migration context without replacing the canonical DomainModel flow.

## Reversibility Strategy

The first executable `WorkbenchChangeSet` for data migration should generate dry-run artifacts first:

- a staging load script;
- a row-count and checksum report;
- a rollback note requiring either a pre-load snapshot or staging-table swap.

Production data migration execution remains out of scope for this first cut, matching the ticket's boundary.
