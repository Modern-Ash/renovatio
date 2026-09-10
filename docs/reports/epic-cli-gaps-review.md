# Review report: Epic #152 CLI profile and generation gaps

Date: 2026-09-04
Reviewer role: developer self-review
Agora work: `decision-engine-epic-gaps/epic-cli-gaps`

## Scope reviewed

- CLI command registration, help, output, exit codes, profile parsing, and state writes.
- Provider-registry STUBS routing and `StubResult` serialization.
- COBOL provider tool publication and target-emitter persistence behavior.
- Node emitter availability in the headless CLI context.
- Profile-template binding and sparse overlay precedence.
- Regression tests and user-facing documentation.

## Findings and disposition

1. **Resolved — template rebinding materialized inherited values.** `profile apply` now stores only
   the immutable template reference. `ReusableProjectStore.effectiveProfile()` reloads the bound
   template and resolves it with the unchanged sparse overlay. A regression test covers A-to-B
   rebinding with a local override.
2. **Resolved — new subcommand help returned usage errors.** `generate` and `profile init` now enable
   Picocli standard help options and their packaged-JAR help exits successfully.
3. **Resolved — ambiguous profile file extensions.** `generate` accepts only `.json`, `.yaml`, and
   `.yml`; unsupported extensions fail before project state changes.

No unresolved correctness, compatibility, or security finding was identified in the changed
scope. Relative output paths are normalized below the workspace by default; explicit absolute
paths remain supported as requested by the caller.

## Criteria review

| Criterion | Assessment |
| --- | --- |
| `profile-init` | Met |
| `explicit-generation` | Met |
| `target-availability` | Met |
| `overlay-rebinding` | Met |
| `cli-contract` | Met |
| `regression-quality` | Met, with the unrelated repository-wide JaCoCo baseline documented in the verification report |

Final Spec Owner acceptance is intentionally not asserted by this self-review and remains a human
gate.
