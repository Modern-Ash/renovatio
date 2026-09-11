# Issue 230 Implementation Plan

## Completed Slice

1. Add a framework-free capability registry to `renovatio-application`.
2. Expose the registry through API, CLI and MCP without copying capability semantics.
3. Keep unsupported targets truthful by marking them `planned` instead of runnable.
4. Add contract tests for application, API, CLI and MCP.

## Remaining Follow-ups

- Generate OpenAPI schemas from the same contract instead of relying on controller serialization alone.
- Wire Workbench to `GET /api/v1/capabilities` when Workbench capability gating work begins.
- Replace legacy provider-generated tool catalogs with application capability metadata where behavior is already behind `ApplicationCommandBus`.
