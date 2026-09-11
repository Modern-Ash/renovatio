# Issue 232 Implementation Plan

## Steps

1. Preserve existing multi-program deduplication behavior for byte-identical shared Node artifacts.
2. Extend the default Node renderer with deterministic build artifacts:
   - `src/main.test.ts`
   - `package-lock.json`
   - package scripts for `build`, `lint`, `test`, and `start`
3. Remove the generated Express dependency from the shared bootstrap and use Node's built-in HTTP
   module.
4. Add focused tests for:
   - shared project artifact equality across programs;
   - exact package scripts and lockfile presence;
   - API preview service output;
   - capability metadata keeping Node experimental.
5. Run focused Maven suites covering emitter, provider aggregation, CLI, API service, and capability
   contracts.

## Agora Note

GitHub issue #232 declares `architecture-convergence-2026/node-emitter-contract`, but the current
Agora checkout has `architecture-convergence-2026` at `.agora/swarms/048-architecture-convergence-2026`
while the Agora CLI attempts to read `.agora/swarms/002-architecture-convergence-2026`. Work creation
therefore fails before code changes. This cycle records the issue artifacts in repo docs and leaves
the durable Agora index repair as follow-up governance work.
