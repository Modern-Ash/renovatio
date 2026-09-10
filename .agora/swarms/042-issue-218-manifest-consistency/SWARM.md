---
schema: "agora/swarm/v1"
id: "issue-218-manifest-consistency"
method: "spec-driven"
status: "cancelled"
branch: "agora/issue-218-manifest-consistency"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm issue-218-manifest-consistency

## Objective

Fix issue #218: JavaGenerationRegistryRoutingTest has 2 failures on main — previewArchitecture() and generateInterfaceStubs() diverge on the emitted artifact manifest. (1) A CICS program's planned RoutedCicsController.java is rejected by manifest validation during stub emission (TARGET_MANIFEST_MISMATCH). (2) Architecture-canvas custom package roots feed the preview but not generation, which emits modules/<module>/... default paths instead. Align both code paths so the preview manifest and the emitted manifest are identical for CICS controllers and canvas package roots, with no regression to routing or the rest of renovatio-provider-cobol.

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
