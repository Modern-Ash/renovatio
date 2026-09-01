# F1 Decision Layer — Verification Report

- **Work item:** `decision-engine-f1/f1-decision-layer`
- **Specification:** `docs/specs/f1-decision-layer.md`
- **Implementation plan:** `docs/plans/f1-decision-layer.md`
- **Tested commit:** `6929212d5d04b97a2c16ebddc5b40aef171499b1`
- **Verification date:** 2026-09-01
- **Overall result:** PASS WITH BASELINE BUILD-GATE EXCEPTION

## Acceptance results

| Criterion | Result | Inspectable support |
|---|:---:|---|
| `profile-contract` | PASS | Profile model/schema, JSON/YAML round trips, validation, merge, canonical hash, legacy importer, and profile tests |
| `decision-contract` | PASS | Stable catalog/ids, transition and resolver tests, persistence adapters, project isolation, re-analysis, retirement, bulk confirmation, and deletion tests |
| `llm-suggestions` | PASS | Six versioned prompts, strict shared output schema, bounded runtime, deterministic fallback/telemetry, cache/cap tests, and zero provider calls for the seven deterministic F1 decisions |
| `api-contract` | PASS | Five routes/six operations, ETag conflicts, role and project isolation, filters, validation/error mapping, and MockMvc contract tests |
| `ui-workflow` | PASS | Target and Decisions steps, exact clients, disabled inactive targets, preview, filters, source/fallback states, mutations, stale refresh, bulk confirmation, and component tests |
| `compatibility` | PASS WITH EXCEPTION | The 13-fixture default-F1 adapter comparison is exact; service hashes match; full reactor tests/build pass with JaCoCo skipped; the literal `mvn clean install` is blocked by pre-existing 100% coverage gates in unchanged base modules |
| `scope-boundaries` | PASS | No Node/Python emitter, architecture transformer, configurable rule engine, data-access classifier, or cross-project profile registry was introduced |

## Commands and results

| Command | Result | Observed |
|---|:---:|---|
| `mvn clean install` | BASELINE FAILURE | `renovatio-shared` reports 90% line coverage against a pre-existing 100% threshold; this branch has no production or test diff in that module |
| `mvn clean install -Djacoco.skip=true` | PASS | All 15 reactor modules built and installed; 408 Java tests passed, 0 failed, 0 errored, 0 skipped |
| `mvn -pl renovatio-mcp-server -am test` | PASS | Reactor success; MCP module 22 tests passed, dependencies also passed |
| focused profile/decision/LLM/API/legacy suite | PASS | All selected F1 tests passed |
| PR 157 review-fix suite | PASS | 24 profile, decision-domain, and API tests passed; includes profile precedence, null extension persistence, and simultaneous decision PATCH coverage |
| `CharacterizationFixtureContractTest` | PASS | 13 fixtures compared twice baseline-vs-F1; generated key sets and UTF-8 bytes are identical |
| `npm test -- --run` | PASS | 11 files, 23 tests passed |
| `npm run lint` | PASS | ESLint completed without findings |
| `npm run build` | PASS | Vite production build completed; 54 modules transformed |
| `git diff --check` | PASS | No whitespace errors |
| post-rebase API reactor and UI matrix | PASS | API reactor tests, 23 UI tests, lint, and production build passed on the tested commit after rebasing onto `origin/main@e487abc` |

The first `mvn clean install` failure reproduces without any
`renovatio-shared` change. A diagnostic attempt to cover its missing lines was
discarded after the next unchanged module, `renovatio-core`, exposed the same
pre-existing 90%-versus-100% gate. No baseline module was altered to weaken or
game coverage. The full reactor was therefore verified with only JaCoCo's gate
disabled, while the required test suites were also run normally.

The PR 157 review pass found and resolved three contract defects. Catalog
defaults remain present in `resolvedDecisions` and the content hash but no
longer override stored profile values until the corresponding decision is
confirmed or overridden. Decision PATCH now keeps read/check/write in one
transaction, flushes the managed JPA version, and maps an optimistic conflict
to HTTP 409; a synchronized two-request test observes exactly one 200 and one
409. Extension maps now retain explicit JSON null values through JSON, YAML,
and persisted API round trips while remaining defensively unmodifiable.

## Compatibility anchors

| Source | Expected SHA-256 | Observed |
|---|---|:---:|
| `JavaGenerationService.java` | `8ec5359ece8a48cc0c8891f235c770a9a5ac7dddc6c79e024f581a32361890c3` | exact |
| `MigrationPlanService.java` | `2e44a17db423b8a70d576aeaa89475f1cfe3e24d057e04fb1ece991dcd4803be` | exact |

The F1 compatibility adapter resolves the default profile and all seven
baseline decision values but remains inert at emission. Across the complete
13-fixture issue-#122 corpus, repeated baseline and F1 runs produce identical
sorted generated-file keys and identical byte arrays for every emitted Java
artifact, including annotated output where present.

## Review notes

- The root POM still emits its pre-existing duplicate
  `renovatio-mcp-server` dependency-management warning.
- Existing compiler warnings (MapStruct, ANTLR version skew, unchecked code,
  and dynamic Mockito agent loading) remain unchanged and non-fatal.
- `npm ci` reports six dependency advisories (four moderate, one high, one
  critical). No forced major-version audit remediation was applied in this F1
  feature because it is a separate dependency-maintenance concern.

## Conclusion

The implemented F1 behavior, compatibility harness, API, UI, MCP regression,
and full reactor test execution are green at the tested commit. The literal
coverage-enforcing `mvn clean install` remains a repository-wide baseline
finding and is recorded as failure evidence rather than represented as an F1
regression. The work is ready for pull-request review in Agora `verifying`.
