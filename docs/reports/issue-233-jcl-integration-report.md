# Issue 233 JCL Integration Report

## Summary

AC-12 closes the primary architectural gap left by F7: the JCL core no longer
depends directly on the LLM runtime, and batch orchestration participates in the
canonical application preview manifest instead of requiring a transport-specific
path.

## Decisions

- `DecisionSuggestionPort` lives in `renovatio-decisions` because decisions are
  the shared governance boundary between deterministic domains and proposal
  providers.
- `DecisionSuggestionService` remains in `renovatio-llm` and implements the
  port. This keeps runtime cache/provider/prompt concerns outside JCL.
- `BatchOrchestrationPlanner` is an application port. Production adapters can
  wire the existing `renovatio-jcl` parser/projection/emitter behind it without
  changing CLI/API/MCP flows.
- The default planner is disabled, preserving byte-stable behavior when batch
  orchestration is not configured.

## Capability Status

JCL/Spring Batch remains experimental. The deterministic core and application
flow are cleaner, but promotion to supported still requires project-specific
manifest and characterization evidence.

## Agora State

The missing `.agora/swarms/002-architecture-convergence-2026/SWARM.md` record
was restored from the existing `architecture-convergence-2026` work materials,
and `jcl-decoupling-integration` was registered and completed through the Agora
CLI.

`agora work inspect --swarm architecture-convergence-2026 --work
jcl-decoupling-integration` now succeeds with state `completed`, 7/7 criteria
satisfied, 5 artifacts, 2 successful evidence entries and one `spec-owner`
approval.

`agora validate` no longer reports `swarm.invalid`; it still reports pre-existing
historical evidence digest drift for unrelated artifacts.
