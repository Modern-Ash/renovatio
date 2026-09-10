# Verification report: Epic #152 CLI profile and generation gaps

Date: 2026-09-04
Agora work: `decision-engine-epic-gaps/epic-cli-gaps`
Branch: `fix/f8-review-findings`

## Result

The implementation satisfies the six governed acceptance criteria. Java and Node generation run
through `LanguageProviderRegistry`, profile initialization is non-destructive, template rebinding
preserves sparse local overrides, and unsupported targets fail closed.

## Automated verification

| Check | Result | Detail |
| --- | --- | --- |
| Full Java reactor | PASS | `mvn -Dexec.skip=true test`; all modules, 585 tests |
| Installable reactor | PASS | `mvn clean install -Djacoco.skip=true`; 20/20 modules |
| Focused CLI/provider tests | PASS | `GenerateCommandTest`, `ReusableCommandsTest`, and registry routing coverage |
| Characterization guardrail | PASS | `CharacterizationFixtureContractTest` |
| UI tests | PASS | 12 files, 28 tests |
| UI production build | PASS | Vite build, also exercised by the Maven reactor |
| Packaged CLI smoke | PASS | Root help lists `generate`; `generate --help` and `profile init --help` exit 0 |
| Whitespace validation | PASS | `git diff --check` |

## Acceptance evidence

- `profile-init`: a first initialization writes a sparse v1 overlay; a second returns exit 1 and
  preserves the existing file; `--force` replaces it.
- `explicit-generation`: JSON Java and YAML Node profiles generate real files into selected output
  directories through `cobol.stubs`.
- `target-availability`: the packaged CLI registers Java and Node; Python returns
  `TARGET_EMITTER_UNAVAILABLE` and reports the available targets.
- `overlay-rebinding`: switching template A (Java) to B (Node) changes the inherited target while a
  local `HEXAGONAL` override remains unchanged.
- `cli-contract`: commands are registered, documented, expose standard help, and use 0/1 exit codes.
- `regression-quality`: the checks above pass without changing the characterization fixtures.

## Known baseline gate

The literal `mvn clean install` still stops in the pre-existing `renovatio-shared` JaCoCo check
because that module requires 100% instruction coverage and currently reports about 90%. This is
unrelated to the files changed here. Tests and packaging pass when only that inherited coverage
threshold is skipped; no product test is skipped.
