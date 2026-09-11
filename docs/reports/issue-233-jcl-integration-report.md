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

`agora work inspect --swarm architecture-convergence-2026 --work
jcl-decoupling-integration` cannot currently inspect the declared work because
Agora resolves the swarm to `.agora/swarms/002-architecture-convergence-2026`,
which is absent in this checkout. `agora validate` also reports pre-existing
stale evidence digests. This implementation therefore records artifacts in the
repository and GitHub PR, while durable Agora closure remains blocked by project
state repair.
