# Specification: Node multi-program generation

> GitHub epic: [#152](https://github.com/Modern-Ash/renovatio/issues/152)
> Node emitter work: [#150](https://github.com/Modern-Ash/renovatio/issues/150)
> Pull request: [#168](https://github.com/Modern-Ash/renovatio/pull/168)
> Agora work: `decision-engine-node-multiprogram/node-multiprogram-generation`

## Outcome

Make the existing Node target composable at project scope. A workspace containing multiple COBOL
programs must generate one Node/TypeScript application: each program contributes its canonical
architecture artifacts, while the application bootstrap and manifests are emitted once.

The change closes the concrete multi-program collision gap in F5. It does not expand semantic IR,
change the Java emitter, or claim full behavioral translation of COBOL statements.

## Per-program artifact contract

- `DefaultNodeRenderer` consumes the canonical paths in `TargetModel.targetStructure().artifactPaths()`.
- Every canonical `.ts` path receives deterministic, non-empty TypeScript content derived only from
  the target model, profile, planned path, and normalized program identifier.
- Service/use-case, entity, repository/port, and controller/adapter paths receive valid standalone
  TypeScript declarations appropriate to their suffix. Unknown planned TypeScript roles receive a
  deterministic exported program descriptor.
- Path and TypeScript identifiers are normalized without allowing source-controlled path traversal.
- Distinct programs therefore contribute distinct files when the architecture planner assigns
  distinct module paths.

## Shared project artifact contract

Every program render includes the same byte-identical project files:

- `src/main.ts`: Express bootstrap with a project-level `/health` endpoint;
- `package.json`: stable project manifest and build/start scripts; and
- `tsconfig.json`: stable strict compiler configuration.

Shared content must not contain the current program identifier. Project aggregation deduplicates an
artifact path only when the existing and incoming contents are equal.

## Collision and persistence contract

- Equal path/equal content is an intentional shared artifact and is retained once in insertion order.
- Equal path/different content remains an error containing `duplicate artifact path`.
- All artifacts are aggregated in memory before the existing disk-write boundary, so a collision
  failure does not create the selected output tree.
- Successful explicit Node generation persists the complete deduplicated project tree.

## Compatibility and exclusions

- Single-program Node generation retains `src/main.ts`, `package.json`, and `tsconfig.json` and gains
  the canonical planned TypeScript artifacts.
- Java aggregation continues to reject conflicting duplicate paths and its emitted bytes are not
  changed by this work.
- No changes are made to COBOL IR, semantic IR, architecture planning, profile precedence, Prisma,
  dependency installation, or generated-project runtime execution.

## Acceptance scenarios

1. Two target models rendered for Node have byte-identical shared artifacts and disjoint canonical
   per-program paths.
2. A two-program COBOL workspace generated through the CLI succeeds and writes both programs'
   TypeScript artifacts plus one copy of each shared file.
3. A synthetic emitter returning the same shared path and content for two programs is deduplicated.
4. A synthetic emitter returning the same path with differing content fails and writes no selected
   output directory.
5. Focused Node emitter, provider aggregation, CLI, and relevant Maven reactor tests pass.

