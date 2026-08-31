# Annotated OpenRewrite Pass Test Report

Date: 2026-08-31  
Agora work: `ai-modernization/annotated-openrewrite-pass`  
GitHub issue: #127

## Scope

This report verifies deterministic consumption of validated `cobol-annotated-ir.v1` sidecars by
the OpenRewrite COBOL translation pass. It covers accepted `DOMAIN_NAMING` and `DATA_INTENT`
applications, review and validation fallback outcomes, provider-free recipe boundaries, offline
reproducibility, and the end-to-end generation/reporting path.

## Commands and results

The focused implementation suites were run offline throughout development. Final verification used:

```text
mvn -q -pl renovatio-cobol-annotations,cobol-openrewrite-recipes,renovatio-cobol-ir,renovatio-cobol-runtime,renovatio-provider-cobol test -o -Djacoco.skip=true
```

Result: success, 160 tests, 0 failures, 0 errors, 0 skipped.

| Module | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| `renovatio-cobol-annotations` | 2 | 0 | 0 | 0 |
| `cobol-openrewrite-recipes` | 21 | 0 | 0 | 0 |
| `renovatio-cobol-ir` | 55 | 0 | 0 | 0 |
| `renovatio-cobol-runtime` | 23 | 0 | 0 | 0 |
| `renovatio-provider-cobol` | 59 | 0 | 0 | 0 |

The CI isolation lane was then reproduced with the workflow's pinned Maven/Temurin 17 image,
`--network=none`, no provider credential variables, and a clean build:

```text
mvn -B -o -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am clean test -Djacoco.skip=true
```

Result: `BUILD SUCCESS` for all nine reactor projects. DNS and direct TCP isolation checks failed
closed as expected before Maven ran. The COBOL provider reported 59 passing tests, including the
annotated characterization fixtures. An earlier host-Java-21 run encountered stale generated
module output in `renovatio-shared`; the clean pinned-Java-17 CI reproduction eliminated that local
artifact issue without any source change.

The recipe boundary and Maven Enforcer verification also passed offline:

```text
mvn -q -pl cobol-openrewrite-recipes verify -o -DskipTests=false -Djacoco.skip=true
```

The boundary test was mutation-checked by temporarily introducing a forbidden
`org.shark.renovatio.provider` reference in the annotations module: the test failed, and passed again
after the probe was removed.

## Acceptance evidence

### `annotated-consumption`

- `PopulateCobolProcessRecipeAnnotatedTest` proves the recipe reads the annotated context through
  `ExecutionContext`, applies an accepted rename, and publishes dropped outcomes.
- `AnnotatedContextResolverTest` proves inline, explicit-path, sibling-path, and legacy precedence
  with strict schema, semantic, and base-hash validation.
- `JavaGenerationServiceAnnotatedTest` proves a committed sibling sidecar changes both the DTO and
  service implementation and writes deduplicated manual action items.

### `ast-safe`

- `AnnotationApplicatorDomainNamingTest` proves field, accessor, and invocation renames plus
  collision/invalid-identifier drops.
- `AnnotationApplicatorDataIntentTest` proves marker attachment through an OpenRewrite
  `JavaTemplate` and deterministic output.
- No production annotation application path performs raw source-string replacement.

### `no-provider-call`

- `PopulateCobolProcessRecipeTest.productionBoundaryShouldContainNoNetworkOrLlmDependency` scans
  both production modules and rejects provider, HTTP, credential, LLM, and prompt dependencies.
- Maven Enforcer bans provider and network/provider-SDK dependencies transitively from the recipe
  module.
- The full isolated lane passed with network disabled and provider credentials absent.

### `reproducible`

- `CharacterizationFixtureContractTest` runs each annotated translation twice and compares both
  outputs byte-for-byte with committed goldens.
- `move-numeric` proves accepted `DOMAIN_NAMING`; `data-intent-redefines` proves accepted
  `DATA_INTENT`. Both annotated outputs compile and preserve their committed observations.

### `fallback`

- `AnnotationApplicatorEligibilityTest` covers stale hashes and non-accepted review states.
- `AnnotationApplicatorDomainNamingTest` covers collision and invalid-name fallback.
- `AnnotationActionItemFactoryTest` proves stable `manual-action-item.v1` mappings and severity/gate
  selection.
- `CobolSemanticTranspilerTest` proves dropped outcomes are drained to the reporting sink.
- `JavaGenerationServiceAnnotatedTest` proves the report is emitted under
  `build/reports/renovatio/manual-action-items.json` while deterministic translation continues.

## Follow-ups

No acceptance blocker remains for issue #127. Optional idiomatic polish remains governed separately
by issue #128 and is outside this work item.
