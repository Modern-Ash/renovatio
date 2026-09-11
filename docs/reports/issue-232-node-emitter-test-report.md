# Issue 232 Node TargetEmitter Test Report

## Environment

- Branch: `issue-232-node-emitter-contract`
- Base: `origin/main` at `ccb2739bfbbed0250dd497eb28c9949b80ae52e0`
- Java: 21.0.12

## Commands

```bash
mvn -q -pl renovatio-emitter-node -am -Dtest=NodeEmitterTest -Dsurefire.failIfNoSpecifiedTests=false test -Dexec.skip=true
```

Result: passed.

```bash
mvn -q -pl renovatio-emitter-node,renovatio-provider-cobol,renovatio-cli -am -Dexec.skip=true -Dtest=NodeEmitterTest,JavaGenerationRegistryRoutingTest,GenerateCommandTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: passed.

```bash
mvn -q -pl renovatio-application,renovatio-emitter-node,renovatio-api,renovatio-cli -am -Dexec.skip=true -Dtest=SurfaceCapabilityRegistryTest,NodeEmitterTest,NodePreviewServiceTest,CapabilitiesControllerTest,CapabilitiesCommandTest,GenerateCommandTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: passed.

## Coverage

- Node renderer emits deterministic shared build artifacts across programs.
- Multi-program provider and CLI aggregation still deduplicate equal shared files.
- Generated Node target includes `package.json`, `package-lock.json`, `tsconfig.json`,
  `src/main.ts`, `src/main.test.ts`, and `docs/node-idioms.md`.
- Package scripts include build, lint, test, and start commands.
- Generated bootstrap uses built-in Node HTTP APIs instead of adding an Express runtime dependency.
- API preview service exposes the same build/test artifacts.
- API, CLI, and application capability contracts keep `node.target` experimental.
- `docs/specs/issue-232-node-target-contract.md` records the target artifact and maturity contract.
- `docs/reports/issue-232-node-equivalence-report.md` records structural equivalence coverage and
  the remaining behavioral gate before beta.

## Residual Scope

Node remains experimental. A later cycle must run a clean generated-project `npm install`, build,
lint, test, and equivalence fixture gate before changing maturity to beta.
