---
schema: "agora/work/v1"
id: "f2-semantic-ir-emitter-spi"
swarm: "decision-engine-f2"
title: "F2 \u00b7 Semantic IR and TargetEmitter SPI (issue #147)"
state: "completed"
revision: 1
operational-status: "active"
status-reason: "Spec Owner authorized extending the repository Tool Pack with a constrained governed branch-publication operation."
status-by: "project:agent"
status-at: "2026-09-01T17:38:23.303225Z"
acceptance-criteria: {"semantic-ir":"A new renovatio-semantic-ir module models target-neutral semantic types, data intents, side effects, classified I/O, control flow, and unclassified data access without Java-specific dependencies","intent-projection":"CobolDataIntent remains compatible as a generated Java projection whose source of truth is the semantic IR","emitter-spi":"TargetEmitter, TargetModel, EmittedArtifacts, and TargetEmitterRegistry provide deterministic JAVA selection and clear unavailable-emitter errors for NODE and PYTHON","java-adapter":"JavaEmitter wraps the existing OpenRewrite/template generation path without changing emitted artifact keys or bytes","profile-integration":"The effective F1 MigrationProfile feeds TargetModel and emitter selection without ad-hoc target translation","regression-gates":"The Maven reactor, issue-122 characterization harness, MCP server, and renovatio-cli regressions pass with inspectable evidence","scope-boundaries":"F2 introduces no real Node/Python emitter, architecture transformation, or fine persistence classification"}
satisfied-criteria: ["semantic-ir","intent-projection","emitter-spi","java-adapter","profile-integration","regression-gates","scope-boundaries"]
criterion-statuses: {"semantic-ir":["specified","planned","implemented","verified","accepted"],"intent-projection":["specified","planned","implemented","verified","accepted"],"emitter-spi":["specified","planned","implemented","verified","accepted"],"java-adapter":["specified","planned","implemented","verified","accepted"],"profile-integration":["specified","planned","implemented","verified","accepted"],"regression-gates":["specified","planned","implemented","verified","accepted"],"scope-boundaries":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec"]
child-work-refs: []
budget-limits: null
---

# F2 · Semantic IR and TargetEmitter SPI (issue #147)

## Description

Refactor the COBOL-to-Java path into target-neutral semantic analysis followed by emitter selection, preserving current output and the explicit F2 boundaries from issue #147.

## Acceptance criteria

- [x] **semantic-ir:** A new renovatio-semantic-ir module models target-neutral semantic types, data intents, side effects, classified I/O, control flow, and unclassified data access without Java-specific dependencies; stages: specified, planned, implemented, verified, accepted
- [x] **intent-projection:** CobolDataIntent remains compatible as a generated Java projection whose source of truth is the semantic IR; stages: specified, planned, implemented, verified, accepted
- [x] **emitter-spi:** TargetEmitter, TargetModel, EmittedArtifacts, and TargetEmitterRegistry provide deterministic JAVA selection and clear unavailable-emitter errors for NODE and PYTHON; stages: specified, planned, implemented, verified, accepted
- [x] **java-adapter:** JavaEmitter wraps the existing OpenRewrite/template generation path without changing emitted artifact keys or bytes; stages: specified, planned, implemented, verified, accepted
- [x] **profile-integration:** The effective F1 MigrationProfile feeds TargetModel and emitter selection without ad-hoc target translation; stages: specified, planned, implemented, verified, accepted
- [x] **regression-gates:** The Maven reactor, issue-122 characterization harness, MCP server, and renovatio-cli regressions pass with inspectable evidence; stages: specified, planned, implemented, verified, accepted
- [x] **scope-boundaries:** F2 introduces no real Node/Python emitter, architecture transformation, or fine persistence classification; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
