# Issue 232 Node TargetEmitter Contract

## Scope

Issue #232 keeps Node/TypeScript experimental while making the generated target tree more honest and
verifiable. This cycle does not promote Node to beta and does not add new COBOL semantics.

## Contract

- Node output must keep program-scoped TypeScript artifacts separate from byte-identical shared
  project artifacts.
- Shared project artifacts must be deterministic across programs so multi-program aggregation can
  deduplicate them safely.
- Generated project files must include a package manifest, lockfile marker, TypeScript compiler
  configuration, build script, lint script, and smoke test script.
- Runtime bootstrap should avoid unnecessary generated dependencies when built-in Node APIs are
  sufficient.
- Capability metadata must continue to identify `node.target` as experimental across API and CLI.

## Non-Goals

- No beta maturity claim.
- No remote package installation during Java unit tests.
- No hidden Prisma or TypeORM selection outside confirmed profile decisions.
- No new COBOL parser or Semantic IR behavior.
