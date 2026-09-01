# F2 Semantic IR and TargetEmitter SPI — Verification Report

- **Agora work:** `decision-engine-f2/f2-semantic-ir-emitter-spi`
- **GitHub issue:** #147
- **Reviewed implementation:** `7e47d6c9b117827f68384ebf20f1499cf03f60c4`
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

## Verification matrix

| Gate | Command / proof | Result |
|---|---|---|
| Semantic/shared contracts | `mvn -pl renovatio-shared -am test` | PASS — semantic IR 5, profile 7, shared 26 |
| Architecture and registry | `F2ArchitectureTest`, `TargetEmitterRegistryTest`, structured error-path tests in the functional reactor | PASS — ArchUnit 2; registry 3; error paths 5 |
| COBOL projection and provider | `mvn -pl renovatio-provider-cobol -am test` | PASS — provider 90; all 12 modules green |
| Issue #122 corpus | `mvn -pl renovatio-provider-cobol -am -Dtest=CharacterizationFixtureContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS — 13 fixtures traverse the F2 boundary; 2 supported byte comparisons, 11 residual empty emissions |
| Annotation projection | `AnnotationApplicatorDataIntentTest` and the annotated characterization fixture | PASS — neutral and compatibility paths emit identical Java source bytes |
| Multi-program aggregation | `JavaGenerationRegistryRoutingTest` | PASS — 3 tests, including duplicate-path rejection and fail-closed NODE selection |
| Java-producing provider tools | `CobolLanguageProviderEmitterRoutingTest` | PASS — copybook, DB2, and control-break routes stop on unavailable NODE target |
| MCP and CLI regression | `mvn -pl renovatio-mcp-server,renovatio-cli -am test` | PASS — MCP 22, CLI 18; 14-module reactor green |
| Source hygiene | `git diff --check` | PASS |

The full provider reactor also recorded these upstream counts: core 37,
provider-java 14, COBOL runtime 23, COBOL IR 55, annotations 2, recipes 25,
decisions 8, and provider-cobol 90. No fixture golden or JaCoCo threshold was
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

1. Commit and register this report as `test-report` and successful evidence.
2. Transition the work to `verifying` and open the PR for external review.
3. Resolve review comments, rerun affected gates, and only then seek the Spec
   Owner completion approval.
