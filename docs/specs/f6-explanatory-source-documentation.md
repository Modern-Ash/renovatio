# F6 follow-up: deterministic explanatory source documentation

> GitHub epic: [#152](https://github.com/Modern-Ash/renovatio/issues/152)
> F6 issue: [#151](https://github.com/Modern-Ash/renovatio/issues/151)
> Pull request: [#168](https://github.com/Modern-Ash/renovatio/pull/168)
> Agora work: `decision-engine-f6-documentation/explanatory-source-documentation`

## Outcome

Close the documentation-emission seam explicitly deferred by the accepted F6 specification. A
project may enable deterministic explanatory documentation for generated Java and Node program
units. The documentation identifies the original COBOL program and source, records the effective
translation choices, and links their applied `DecisionPoint` identifiers.

This increment consumes only validated `TargetModel` data. It does not let an LLM write source text,
does not invent rationale absent from the effective decision envelope, and does not alter executable
statements.

## Profile contract

- The extension key is `documentation.enabled`.
- The value must be a JSON/YAML boolean. Strings and numbers are rejected with
  `/extensions/documentation.enabled` and `INVALID_TYPE`.
- The key is absent by default, which is equivalent to `false` and preserves existing output bytes.
- A typed `DocumentationSettings.enabled(MigrationProfile)` accessor is the sole interpretation of
  the extension.

## Documentation content

For one `TargetModel`, the canonical documentation block contains:

1. the normalized COBOL program identifier;
2. the normalized source path from `SourceProvenance`;
3. every effective decision as `key=value`, ordered by key; and
4. every applied `DecisionPoint` SHA-256 identifier, ordered lexicographically.

The profile hash is intentionally omitted from source prose: decision choices and identifiers are
the reviewable contract. When either collection is empty, the block records `none` rather than
omitting the field.

All dynamic text is single-line normalized. Comment terminators, carriage returns, line feeds, and
other ISO control characters are replaced so source-controlled values cannot terminate or inject a
documentation block.

## Target application

### Java

- `JavaEmitter` decorates every emitted `.java` artifact only when enabled.
- The Javadoc is inserted after package/import declarations and before annotations or the top-level
  declaration, so it documents the generated compilation unit's primary type.
- Non-Java artifacts pass through byte-for-byte.

### Node

- `DefaultNodeRenderer` prepends TSDoc to every program-specific planned `.ts` artifact when enabled.
- Project-level `src/main.ts`, `package.json`, and `tsconfig.json` remain program-independent and are
  never decorated. This preserves safe multi-program deduplication.
- When disabled, every Node artifact remains byte-identical to the established renderer output.

## Invariants and exclusions

- Same target model and profile produce identical documentation bytes.
- Documentation is derived from validated model data, never raw LLM prose.
- No change to COBOL IR, semantic IR, `TargetEmitter` SPI, decision state transitions, or generated
  executable behavior.
- Creating a new LLM documentation prompt or persistence/cache path remains separate work; accepted
  LLM decisions can already be traced by their `DecisionPoint` identifiers here.

## Acceptance scenarios

1. Profile parsing rejects a non-boolean `documentation.enabled` and typed access defaults false.
2. Enabled Java emission inserts one escaped, deterministically ordered Javadoc block into every
   Java artifact and leaves other artifacts unchanged.
3. Enabled Node emission inserts escaped TSDoc in every planned program unit while shared project
   files remain byte-identical across two programs.
4. Disabled Java and Node emission returns exactly the renderer bytes used before this increment.
5. Focused profile/shared/emitter tests, full Maven tests, clean install, and patch hygiene pass.
