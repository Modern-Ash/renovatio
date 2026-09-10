---
schema: "agora/work/v1"
id: "f4-persistence-strategy"
swarm: "decision-engine-f4"
title: "F4 \u00b7 PersistenceStrategy pluggable (issue #149)"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"data-access-classifier":"DataAccessClassifier produces DataAccessClassification records for all six DataAccessKind values from F2 SemanticProgram","persistence-strategy-spi":"PersistenceStrategy interface supports() and emit() implemented by JPA, SPRING_DATA_JDBC, IN_MEMORY strategies","profile-integration":"persistence.sourceStrategies map in MigrationProfile merges with defaults and per-source overrides","api-contract":"GET /api/projects/{id}/data-accesses returns classified accesses with confidence and suggested strategy","wizard-step":"Persistence wizard step shows detected sources table with per-source strategy dropdown","orchestration":"Pipeline integrates classifier before emitter, aggregating persistence artifacts into emitted output","compatibility":"Default profile with no sourceStrategies produces byte-identical output to F3","verification-scope":"All \u00a712 verification gates pass with inspectable evidence"}
satisfied-criteria: ["data-access-classifier","persistence-strategy-spi","profile-integration","api-contract","wizard-step","orchestration","compatibility","verification-scope"]
criterion-statuses: {"data-access-classifier":["specified","planned","implemented","verified","accepted"],"persistence-strategy-spi":["specified","planned","implemented","verified","accepted"],"profile-integration":["specified","planned","implemented","verified","accepted"],"api-contract":["specified","planned","implemented","verified","accepted"],"wizard-step":["specified","planned","implemented","verified","accepted"],"orchestration":["specified","planned","implemented","verified","accepted"],"compatibility":["specified","planned","implemented","verified","accepted"],"verification-scope":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","test-report"]
child-work-refs: []
budget-limits: null
---

# F4 · PersistenceStrategy pluggable (issue #149)

## Description

Pluggable PersistenceStrategy SPI with data-access classifier (VSAM/sequential/EXEC SQL/flat-file), JPA/SPRING_DATA_JDBC/IN_MEMORY strategies, GET /api/projects/{id}/data-accesses endpoint, and Persistence wizard step. Depends on F2 semantic-IR. Exclude real data migration, DDL generation, IMS/hierarchical, MyBatis/jOOQ.

## Acceptance criteria

- [x] **data-access-classifier:** DataAccessClassifier produces DataAccessClassification records for all six DataAccessKind values from F2 SemanticProgram; stages: specified, planned, implemented, verified, accepted
- [x] **persistence-strategy-spi:** PersistenceStrategy interface supports() and emit() implemented by JPA, SPRING_DATA_JDBC, IN_MEMORY strategies; stages: specified, planned, implemented, verified, accepted
- [x] **profile-integration:** persistence.sourceStrategies map in MigrationProfile merges with defaults and per-source overrides; stages: specified, planned, implemented, verified, accepted
- [x] **api-contract:** GET /api/projects/{id}/data-accesses returns classified accesses with confidence and suggested strategy; stages: specified, planned, implemented, verified, accepted
- [x] **wizard-step:** Persistence wizard step shows detected sources table with per-source strategy dropdown; stages: specified, planned, implemented, verified, accepted
- [x] **orchestration:** Pipeline integrates classifier before emitter, aggregating persistence artifacts into emitted output; stages: specified, planned, implemented, verified, accepted
- [x] **compatibility:** Default profile with no sourceStrategies produces byte-identical output to F3; stages: specified, planned, implemented, verified, accepted
- [x] **verification-scope:** All §12 verification gates pass with inspectable evidence; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- test-report
