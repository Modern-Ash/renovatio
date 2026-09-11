# Issue 232 Node Target Contract

## Target Status

`node.target` remains `experimental`.

## Required Shared Artifacts

The Node renderer must emit these byte-identical files for every program in a project:

- `src/main.ts`
- `src/main.test.ts`
- `package.json`
- `package-lock.json`
- `tsconfig.json`
- `docs/node-idioms.md`

Project aggregation may deduplicate equal path/equal content artifacts. Equal path/different content
is a collision and must fail before writing a partial output tree.

## Required Program Artifacts

Program-scoped TypeScript artifacts come from `TargetStructure.artifactPaths()` and must stay
program-specific. Documentation comments may decorate program artifacts when documentation is
enabled, but must not alter shared project artifacts.

## Build Scripts

The generated `package.json` must include:

- `build`: TypeScript compilation.
- `lint`: TypeScript no-emit validation.
- `test`: build plus Node's built-in test runner.
- `start`: execute the compiled entrypoint.

## Capability Maturity

API and CLI capability contracts must expose Node as experimental until generated-project clean
install, build, lint, test, and equivalence gates are recorded against committed fixtures.
