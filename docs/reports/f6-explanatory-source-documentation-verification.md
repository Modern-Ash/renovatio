# Verification: F6 explanatory source documentation

## Result

PASS. Opt-in explanatory source documentation is deterministic, traceable to the effective target
envelope, comment-safe, and disabled by default. Java and Node emission behavior meets the specified
target boundaries without changing executable statements.

## TDD evidence

The first focused run failed at test compilation because `DocumentationSettings` and
`TranslationDocumentation` did not exist. The tests were retained while the profile contract,
shared formatter, Java decorator, and Node integration were implemented; the same focused command
then passed.

## Automated verification

| Gate | Command | Result |
| --- | --- | --- |
| Focused regression | `mvn -q -pl renovatio-profile,renovatio-shared,renovatio-provider-java,renovatio-emitter-node -am -Dexec.skip=true -Dtest=MigrationProfilesTest,TargetEmissionContractTest,JavaEmitterTest,NodeEmitterTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS, 18 tests |
| Full reactor | `mvn -q -Dexec.skip=true test` | PASS, 591 tests; 0 failures, 0 errors, 0 skipped |
| Clean install | `mvn -q clean install -Djacoco.skip=true -Dexec.skip=true` | PASS |
| Patch hygiene | `git diff --check` | PASS |

The build retained existing module-path, dynamic-agent, model, and test-runtime warnings; none was
introduced as a failing condition by this increment.

## Acceptance coverage

- Profile tests prove absent/false semantics, boolean enablement, and rejection of string values.
- Shared contract tests prove canonical decision/reference ordering, repeatability, and sanitization
  of line breaks and comment terminators.
- Java emitter tests prove placement after package/import declarations, placement before type
  annotations, headerless-source handling, non-Java pass-through, and disabled byte compatibility.
- Node renderer tests prove TSDoc on program-specific units and identical undecorated shared files
  across two programs; existing disabled renderer tests remain green.
