# Issue 230 Test Report

## Commands

- `mvn -pl renovatio-application,renovatio-cli,renovatio-api,renovatio-mcp-server -am test -DskipITs`
  - Result: failed first on `RenovatioCliSmokeTest` before API due missing `mixinStandardHelpOptions` on the new CLI command.
  - Fix: added standard help options to `CapabilitiesCommand`.

- `mvn -pl renovatio-cli,renovatio-api -am test -DskipITs`
  - Result: CLI and dependencies passed. API stopped before tests at `npm run build` because `vite` is not installed in `renovatio-ui`.

- `mvn -pl renovatio-api -am -Dexec.skip=true -Dtest=CapabilitiesControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: passed; 1 test, 0 failures.

- `mvn -pl renovatio-application,renovatio-cli,renovatio-mcp-server -am -Dtest=SurfaceCapabilityRegistryTest,CapabilitiesCommandTest,McpToolingServiceCapabilityTest,RenovatioCliSmokeTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: passed before the API module; 8 tests, 0 failures.

- `mvn -pl renovatio-application,renovatio-cli,renovatio-api,renovatio-mcp-server -am -Dtest=SurfaceCapabilityRegistryTest,CapabilitiesCommandTest,CapabilitiesControllerTest,McpToolingServiceCapabilityTest,RenovatioCliSmokeTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: application, CLI and MCP focal tests passed; API was not reached because the API module invokes the legacy UI build and `vite` is not installed in `renovatio-ui`.
  - API follow-up was verified with `-Dexec.skip=true` above.

- `test -d renovatio-ui/node_modules && npm --prefix renovatio-ui test -- --run src/api/__tests__/client.test.js`
  - Result: not executed; `renovatio-ui/node_modules` is absent in this worktree.

- `test -d renovatio-workbench/node_modules && npm --prefix renovatio-workbench --workspace @renovatio/core-ui test`
  - Result: not executed; `renovatio-workbench/node_modules` is absent in this worktree.

## Coverage Added

- `SurfaceCapabilityRegistryTest`
  - Verifies versioned document identity, supported surfaces, maturity values, common authorization, lifecycle states, canonical error codes, manifest hash fields, and runtime support declarations for LLM, persistence and equivalence.
- `CapabilitiesCommandTest`
  - Verifies CLI JSON output comes from the same contract and exposes common semantics.
- `CapabilitiesControllerTest`
  - Verifies `/api/v1/capabilities` exposes the shared contract and common semantics.
- `McpToolingServiceCapabilityTest`
  - Verifies MCP lists and executes `renovatio.capabilities` using the shared contract and metadata version.
- CLI smoke registration for `capabilities`
- `renovatio-ui` client test for `/api/v1/capabilities`
- `renovatio-workbench` contract test for capability discovery through public API

## Snapshot

See `docs/reports/issue-230-capabilities-snapshot.json`.
