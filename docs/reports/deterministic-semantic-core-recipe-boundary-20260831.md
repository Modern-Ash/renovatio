# Deterministic Semantic Core Recipe Boundary — 2026-08-31

## Scope

- Agora work: `ai-modernization/deterministic-semantic-core`
- Base commit: `03e6b0d`
- Runtime: OpenJDK 17.0.20
- Result: PASS

This checkpoint adds focused executable coverage for the `pure-recipes` acceptance criterion. It
does not claim that the complete issue #122 characterization corpus or offline gate is finished.

## Command

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
  mvn -B -pl cobol-openrewrite-recipes -am \
  -Dtest=PopulateCobolProcessRecipeTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## Results

| Test class | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| `PopulateCobolProcessRecipeTest` | 4 | 0 | 0 | 0 |

Maven reported `BUILD SUCCESS` for the five-project reactor on Java 17.

The new checks prove that:

- two independent parser and OpenRewrite recipe executions produce byte-identical Java and the
  same SHA-256 digest;
- the recipe module production POM and Java sources contain no provider SDK, prompt catalog,
  credential, HTTP-client, or `java.net` dependency;
- the existing MOVE, IF, EVALUATE, and simple PERFORM recipe behavior remains green.

## Remaining lifecycle gate

The work remains `implementing`. Transition to `verifying` still requires the issue #122 fixture
harness and offline characterization gates referenced by the accepted #123 specification, plus
the complete construct-to-test matrix for the deterministic semantic subset.
