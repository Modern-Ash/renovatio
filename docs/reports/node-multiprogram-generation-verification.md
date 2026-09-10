# Verification: Node multi-program generation

## Result

PASS. The Node target now generates deterministic per-program TypeScript artifacts while
byte-identical project files are retained once during workspace aggregation. Conflicting duplicate
paths still fail before the output directory is written.

## TDD evidence

- The initial provider regression failed because two programs both emitted `src/main.ts` and the
  aggregator rejected the second copy as a duplicate.
- After equal-content deduplication, the CLI regression exposed that Node architecture paths were not
  reaching the renderer and produced only the three shared files.
- Wiring the Node layout planner then exposed a same-program service/use-case path collision; the
  planner now retains one canonical artifact per path in stable order.

## Automated verification

| Gate | Command | Result |
| --- | --- | --- |
| Focused regression suite | `mvn -q -pl renovatio-emitter-node,renovatio-provider-cobol,renovatio-cli -am -Dexec.skip=true -Dtest=NodeEmitterTest,JavaGenerationRegistryRoutingTest,GenerateCommandTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS, 17 tests |
| Full reactor tests | `mvn -q -Dexec.skip=true test` | PASS, 587 tests; 0 failures, 0 errors, 0 skipped |
| Clean package/install | `mvn -q clean install -Djacoco.skip=true -Dexec.skip=true` | PASS |
| Patch hygiene | `git diff --check` | PASS |

Maven continued to report pre-existing model and module-path warnings. No new warning was promoted
to a failure by this change.

## Acceptance coverage

- Renderer tests assert canonical service, entity, repository, and controller output and identical
  `src/main.ts`, `package.json`, and `tsconfig.json` bytes across programs.
- Provider tests assert equal-content shared-file deduplication and conflicting-content rejection
  without a partial output tree.
- The CLI test generates a two-program COBOL workspace and verifies both Node service files on disk.
- Existing single-program and Java routing tests remain green.
