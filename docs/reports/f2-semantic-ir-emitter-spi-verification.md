# F2 Semantic IR and TargetEmitter SPI — Verification Report

- **Agora work:** `decision-engine-f2/f2-semantic-ir-emitter-spi`
- **GitHub issue:** #147
- **Reviewed implementation:** `484bf70757c8b24ca7a5b2ddf5d2fb9e545a4341`
- **F1 baseline:** `152d57462d4d7fc2b4554b6a1f029ae44e96d97a`
- **Toolchain:** Apache Maven 3.9.12; OpenJDK 21.0.12; source release 17
- **Result:** review-ready; Agora completion remains pending PR review

## Findings resolved during review

1. Multi-program generation now reprojects the neutral semantic program for
   each source before applying data intents. Artifact aggregation rejects a
   duplicate path instead of overwriting the earlier artifact.
2. `renovatio-shared` coverage initially fell from the baseline-reported 0.9
   to 0.8 after the new contracts were added. Contract and failure-path tests
   restored the reported ratio to 0.9 without changing JaCoCo configuration.
3. The issue-#122 characterization corpus now sends every fixture through
   semantic projection, `TargetModel`, `TargetEmitterRegistry`, and
   `JavaEmitter`. The two fixtures supported by the production translator
   compare exact key sets and byte arrays; the eleven residual fixtures remain
   deterministic empty emissions with their existing manual-action contracts.

No unresolved critical or high-severity implementation finding remains. The
review gate must still inspect and resolve PR feedback before Agora completion.

## PR #159 review remediation

The six valid review findings received on PR #159 were addressed at
`2d05633a3ce6bc372ef2b3928371e6201c2718d5`:

1. Project generation resolves the F1 effective profile once and preserves its
   target selection through every Java-producing provider route.
2. The COBOL provider uses the application-owned `TargetEmitterRegistry`; the
   request-bound Java adapter supplements registered targets and participates
   in duplicate and availability checks.
3. MOVE, COMPUTE, CALL, IF, and EVALUATE operands now project known data access
   as neutral state read/write effects and retain unknown references as residual.
4. Multi-program workspaces emit one `TargetModel` per source, each with its own
   semantic program and source provenance, before deterministic aggregation.
5. Source provenance includes only accepted annotations that actually produced
   semantic data intent.
6. Copybook semantic projection is fail-closed; parse or annotation failures no
   longer invoke a legacy renderer with an empty semantic envelope.

The four valid findings from the subsequent PR re-review were addressed at
`484bf70757c8b24ca7a5b2ddf5d2fb9e545a4341`:

1. Applying a migration plan now propagates the structured
   `TARGET_EMITTER_UNAVAILABLE` failure instead of reporting a successful run.
2. Control-break routing emits every deterministically ordered COBOL source as
   its own semantic envelope; the Java callback decomposes only that source.
3. Standalone copybooks are projected through a copybook-aware synthetic data
   section, so registered emitters receive their declared semantic fields.
4. DB2 `SELECT`, `FETCH`, and `VALUES` operations are reads; known mutations are
   writes; cursor lifecycle and unclassified statements remain `UNKNOWN`.

## Verification matrix

| Gate | Command / proof | Result |
|---|---|---|
| Semantic/shared contracts | `mvn -pl renovatio-shared -am test` | PASS — semantic IR 5, profile 7, shared 26 |
| Architecture and registry | `F2ArchitectureTest`, `TargetEmitterRegistryTest`, structured error-path tests in the functional reactor | PASS — ArchUnit 2; registry 5; error paths 5 |
| COBOL projection and provider | `mvn -pl renovatio-provider-cobol -am test` | PASS — provider 99; all 12 modules green; 303 tests |
| Issue #122 corpus | `mvn -pl renovatio-provider-cobol -am -Dtest=CharacterizationFixtureContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS — 13 fixtures traverse the F2 boundary; 2 supported byte comparisons, 11 residual empty emissions |
| Annotation projection | `AnnotationApplicatorDataIntentTest` and the annotated characterization fixture | PASS — neutral and compatibility paths emit identical Java source bytes |
| Review regressions | `mvn -pl renovatio-provider-cobol -Dtest=CobolLanguageProviderEmitterRoutingTest,CobolSemanticProjectorTest test` plus the full reactor | PASS — 15 focused tests across the three F2 routing/projector classes; ten findings covered |
| Multi-program aggregation | `JavaGenerationRegistryRoutingTest` | PASS — 5 tests, including per-source provenance, duplicate-path rejection, copybook fail-closed, and NODE selection |
| Java-producing provider tools | `CobolLanguageProviderEmitterRoutingTest` | PASS — 5 tests; effective profile, plan propagation, multi-source control-break routing, and copybook field projection covered |
| API integration | `mvn -pl renovatio-api -am -Dexec.skip=true -Dtest=DecisionLayerApiTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS — 9 tests; Spring profile/registry wiring green |
| MCP and CLI regression | `mvn -pl renovatio-mcp-server,renovatio-cli -am -Dexec.skip=true test` | PASS — MCP 22, CLI 18; 14-module reactor green |
| Source hygiene | `git diff --check` | PASS |

The full provider reactor also recorded these upstream counts: core 39,
provider-java 14, COBOL runtime 23, COBOL IR 55, annotations 2, recipes 25,
decisions 8, and provider-cobol 99. No fixture golden or JaCoCo threshold was
changed.

## Literal install and baseline exception

The required literal command `mvn clean install` was executed outside the
filesystem sandbox so Maven could write to the local repository. It reached
`renovatio-shared`, ran 26 green tests, and failed the unchanged JaCoCo rule:

```text
Rule violated for bundle renovatio-shared: lines covered ratio is 0.9,
but expected minimum is 1.0
```

The same literal command was executed in an isolated worktree at F1 head
`c42a5219a778625d84f047b5e6144ba9331e155f`, whose exact tree is the second
parent incorporated by merge commit `152d57462d4d7fc2b4554b6a1f029ae44e96d97a`.
It ran 23 green shared tests and failed at the same module, rule, reported
ratio, and minimum. This is therefore recorded as the explicit baseline-only
exception allowed by the spec. The threshold remains 1.0.

## Compatibility and scope review

- Default routed Java generation matches the legacy artifact key set and exact
  UTF-8 content.
- `@CobolDataIntent` retains its existing type, placement, member order,
  values, assumption order, escaping, and bytes while neutral data intents are
  the routed source of truth.
- NODE and PYTHON have no emitter and cannot fall back to Java. Structured MCP
  errors preserve `code`, `requestedTarget`, and sorted `availableTargets`.
- The semantic module has no COBOL, Java-target, Spring, persistence,
  OpenRewrite, JavaPoet, template, Jackson, or compiler dependency.
- No Node/Python output implementation, architecture transformation, detailed
  persistence classification, fixture-golden update, or coverage weakening was
  introduced.

## Remaining lifecycle actions

1. Register this review revision as `test-report` and successful evidence.
2. Resolve the four addressed PR re-review threads and await the next external
   review/check cycle.
3. Only after the PR is accepted seek the Spec Owner completion approval.
