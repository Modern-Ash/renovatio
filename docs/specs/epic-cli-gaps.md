# Specification: Epic #152 CLI profile and generation gaps

> GitHub epic: [#152](https://github.com/Modern-Ash/renovatio/issues/152)
> Pull request: [#168](https://github.com/Modern-Ash/renovatio/pull/168)
> Agora work: `decision-engine-epic-gaps/epic-cli-gaps`

## Outcome

Complete the two CLI product gaps called out by epic #152 and correct the profile-template
rebinding defect found during review of PR #168. The CLI must be able to initialize the project
profile artifact and invoke target generation with an explicit JSON or YAML profile through the
same provider boundary used by the other CLI commands.

The existing F1 precedence remains authoritative: defaults < bound template < policy decisions <
sparse project overlay < local decisions. A template binding must never materialize inherited
values into the project overlay.

## Command contract

### `profile init`

```
renovatio profile init [--project <path>] [--force] [--json]
```

- `--project` defaults to the current directory.
- The command creates `.renovatio/migration-profile.json` as the canonical, versioned sparse
  overlay (`schemaVersion: "1"`, empty `extensions`).
- Existing profile state is not overwritten unless `--force` is present.
- Human output names the initialized file; JSON output includes `success`, `profilePath`, and the
  stored profile.

### `generate`

```
renovatio generate <path> --profile <json-or-yaml> [--out <dir>] [--json]
```

- The profile is parsed according to its `.json`, `.yaml`, or `.yml` extension and validated before
  any project state is changed. Other extensions fail closed.
- The validated document becomes the project's canonical sparse overlay, making this explicit
  command reproducible across later `plan` and `apply` invocations.
- Generation routes `cobol.stubs` through `LanguageProviderRegistry`; the CLI does not call the
  concrete COBOL provider directly.
- `workspacePath` and `projectId` are the normalized workspace path. `--out` maps to `outputDir`;
  relative output directories resolve beneath the workspace. Without `--out`, Java uses
  `generated-java-stubs` and other targets use `generated-<target>-stubs`.
- Successful human output reports target language, artifact count, and output directory. JSON
  returns the routed provider result. Provider failures exit 1 and preserve structured target
  availability fields.

## Provider and packaging contract

- `LanguageProviderRegistry` routes the existing `LanguageProvider.STUBS` capability and converts
  `StubResult` into the standard result map without changing existing operations.
- `CobolLanguageProvider` publishes `cobol.stubs` in its tool catalog.
- The executable CLI includes the Node emitter module and registers its target emitter in the
  headless Spring context. Java remains the compatibility adapter; Python continues to fail closed
  until a Python emitter exists.
- Non-Java emitted artifacts are returned in the result and are persisted when the caller selects
  an output directory; the CLI always supplies its explicit or target-derived output directory.

## Template rebinding contract

`profile apply` persists only the selected `TemplateReference`. It returns the resolved effective
profile for feedback but leaves `.renovatio/migration-profile.json` unchanged. Rebinding from A to B
therefore changes inherited values from A to B, while fields explicitly present in the sparse local
overlay continue to win.

## Compatibility and exclusions

- Existing analyze/plan/apply/diff behavior and default Java characterization output remain
  unchanged.
- Existing stored full profiles remain valid and intentionally override template values.
- No Python emitter, marketplace, remote profile store, or interactive profile editor is added.

## Acceptance scenarios

1. `profile init` creates a valid sparse v1 profile and refuses a second initialization without
   `--force`.
2. Applying A and then B keeps the local overlay byte-equivalent and the effective result combines
   B with local overrides.
3. `generate <fixture> --profile java.json --out generated` routes STUBS and produces Java files.
4. `generate <fixture> --profile node.yaml --out generated-node` resolves the registered Node
   emitter and persists Node artifacts.
5. A Python profile returns `TARGET_EMITTER_UNAVAILABLE` with requested and available targets.
6. Focused tests, the full Maven reactor, the characterization guardrail, UI tests/build, and
   whitespace checks pass.
