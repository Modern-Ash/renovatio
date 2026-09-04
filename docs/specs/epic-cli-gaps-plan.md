# Implementation plan: Epic #152 CLI profile and generation gaps

## 1. Preserve profile-layer boundaries

- Add safe project-profile initialization and existence/path queries to `ReusableProjectStore`.
- Add `profile init` with non-destructive default behavior and explicit `--force` replacement.
- Change `profile apply` to persist only the immutable template binding and test A-to-B rebinding
  with a sparse local override.

## 2. Expose target generation through the shared provider boundary

- Publish `cobol.stubs` from `CobolLanguageProvider`.
- Route `STUBS` in `LanguageProviderRegistry` and serialize `StubResult` fields consistently.
- Persist every target's emitted artifact tree through `JavaGenerationService`, retaining the
  existing Java manual-action-item behavior.

## 3. Add the explicit-profile CLI workflow

- Implement `GenerateCommand` with JSON/YAML parsing, pre-write validation, project-state update,
  `--out`, human output, JSON output, and standard exit codes.
- Register the command at the root, add the Node emitter dependency/configuration to the CLI, and
  update the command/state README.

## 4. Verify and review

- Add focused registry/provider tests, profile command tests, generate command tests, and CLI
  context/packaging coverage.
- Run focused Maven modules, the full reactor, characterization tests, UI tests/build, and
  `git diff --check`.
- Record verification and review reports, advance criteria through implemented/verified, and keep
  final Spec Owner acceptance pending explicit human approval.
