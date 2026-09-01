---
schema: "agora/work/v1"
id: "f2-semantic-ir-emitter-spi"
swarm: "decision-engine-f2"
title: "F2 \u00b7 Semantic IR and TargetEmitter SPI (issue #147)"
state: "implementing"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"semantic-ir":"A new renovatio-semantic-ir module models target-neutral semantic types, data intents, side effects, classified I/O, control flow, and unclassified data access without Java-specific dependencies","intent-projection":"CobolDataIntent remains compatible as a generated Java projection whose source of truth is the semantic IR","emitter-spi":"TargetEmitter, TargetModel, EmittedArtifacts, and TargetEmitterRegistry provide deterministic JAVA selection and clear unavailable-emitter errors for NODE and PYTHON","java-adapter":"JavaEmitter wraps the existing OpenRewrite/template generation path without changing emitted artifact keys or bytes","profile-integration":"The effective F1 MigrationProfile feeds TargetModel and emitter selection without ad-hoc target translation","regression-gates":"The Maven reactor, issue-122 characterization harness, MCP server, and renovatio-cli regressions pass with inspectable evidence","scope-boundaries":"F2 introduces no real Node/Python emitter, architecture transformation, or fine persistence classification"}
satisfied-criteria: []
criterion-statuses: {"semantic-ir":["specified","planned","implemented","verified"],"intent-projection":["specified","planned","implemented","verified"],"emitter-spi":["specified","planned","implemented","verified"],"java-adapter":["specified","planned","implemented","verified"],"profile-integration":["specified","planned","implemented","verified"],"regression-gates":["specified","planned","implemented","verified"],"scope-boundaries":["specified","planned","implemented","verified"]}
required-artifacts: ["spec"]
child-work-refs: []
budget-limits: null
---

# F2 · Semantic IR and TargetEmitter SPI (issue #147)

## Description

Refactor the COBOL-to-Java path into target-neutral semantic analysis followed by emitter selection, preserving current output and the explicit F2 boundaries from issue #147.

## Acceptance criteria

- [ ] **semantic-ir:** A new renovatio-semantic-ir module models target-neutral semantic types, data intents, side effects, classified I/O, control flow, and unclassified data access without Java-specific dependencies; stages: specified, planned, implemented, verified
- [ ] **intent-projection:** CobolDataIntent remains compatible as a generated Java projection whose source of truth is the semantic IR; stages: specified, planned, implemented, verified
- [ ] **emitter-spi:** TargetEmitter, TargetModel, EmittedArtifacts, and TargetEmitterRegistry provide deterministic JAVA selection and clear unavailable-emitter errors for NODE and PYTHON; stages: specified, planned, implemented, verified
- [ ] **java-adapter:** JavaEmitter wraps the existing OpenRewrite/template generation path without changing emitted artifact keys or bytes; stages: specified, planned, implemented, verified
- [ ] **profile-integration:** The effective F1 MigrationProfile feeds TargetModel and emitter selection without ad-hoc target translation; stages: specified, planned, implemented, verified
- [ ] **regression-gates:** The Maven reactor, issue-122 characterization harness, MCP server, and renovatio-cli regressions pass with inspectable evidence; stages: specified, planned, implemented, verified
- [ ] **scope-boundaries:** F2 introduces no real Node/Python emitter, architecture transformation, or fine persistence classification; stages: specified, planned, implemented, verified

## Required artifacts

- spec
