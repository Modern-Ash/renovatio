# F3 Architecture Transformation Verification

- **Agora work:** `decision-engine-f3/f3-architecture-transform`
- **GitHub issue:** #148
- **Tested revision:** `9056f104b0f60906be9f59c8ead42790770d14a7`
- **Date:** 2026-09-01
- **Result:** PASS

## Acceptance evidence

| Criterion | Evidence |
|---|---|
| Architecture contract | `renovatio-architecture` is target-neutral and transforms ordered semantic programs plus one effective profile into immutable target models, graph, manifest, diagnostics, and stable identities. Module-boundary tests reject provider, UI, Spring, JavaPoet, OpenRewrite, network, and LLM coupling. |
| Transaction script | Each program produces a deterministic service graph and the default Java route preserves the issue-#122 artifact keys and UTF-8 bytes. The 13-fixture offline characterization corpus remains green. |
| Hexagonal | Proven entries, effects, and types produce ports, use cases, adapters, and model components. Unsafe control flow falls back per program with `ARCHITECTURE_FALLBACK_UNSAFE_CONTROL_FLOW`; unknown evidence does not invent relations. |
| Module grouping | Automated tests cover `BY_PROGRAM`, `BY_DOMAIN`, `SINGLE_MODULE`, manual assignment, copybook evidence, longest prefix, normalization, conflicts, unused rules, and stable ordering. |
| Suggestions | `ArchitectureSuggestionCoordinatorTest` proves only eligible `ARCHITECTURE` decisions consume the profile-bounded suggestion budget, suggestions remain unaccepted, disabled policy makes no runtime call, and control-flow plans require green compilation, schema, and characterization evidence. |
| Target views | The read-only preview API and Target UI consume the same canonical graph and manifest as emission. Tests cover draft selection without persistence, exact request parameters, preview/emission path parity, accessible tree/list/SVG, fallback, loading, stale, empty, and error states. |
| Verification scope | Both transaction-script and hexagonal Java layouts compile with the JDK compiler. Aggregate artifacts are validated before an atomic tree replacement. The full Maven reactor and UI suites pass without adding another active architecture style, target tests, or emitter-SPI changes. |

## Executed checks

| Check | Result |
|---|---|
| `mvn -Dexec.skip=true test` | PASS — all 17 reactor projects, including API, CLI, MCP, architecture, LLM, Java, and COBOL providers. |
| `mvn -pl renovatio-provider-cobol -am -Dexec.skip=true -Dtest=CharacterizationFixtureContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS — both corpus contract methods and all 13 committed fixtures. |
| `mvn -pl renovatio-provider-java,renovatio-provider-cobol -am -Dexec.skip=true -Dtest=JavaArchitectureSourceLayoutTest,GeneratedArtifactTreeWriterTest,JavaGenerationRegistryRoutingTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS — 14 focused tests; includes compilation of both layouts and atomic-write protection. |
| `mvn -pl renovatio-llm -am -Dexec.skip=true -Dtest=ArchitectureSuggestionCoordinatorTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS — 2 governed suggestion/control-flow tests. |
| `mvn -pl renovatio-api -am -Dexec.skip=true -Dtest=ArchitecturePreviewApiTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS — 5 preview API tests. |
| `npm test -- --run` | PASS — 11 files, 25 tests. |
| `npm run build` | PASS — 54 modules transformed; production assets generated successfully. |
| `git diff --check` | PASS. |

## Known baseline observations

Maven continues to report the pre-existing duplicate `renovatio-mcp-server` dependency-management
warning. The repository's historical `mvn verify` JaCoCo threshold behavior was not weakened or
changed by F3; the functional `test` reactor is green.
