# Deterministic Semantic Core Revalidation — 2026-08-31

## Scope

- Agora work: `ai-modernization/deterministic-semantic-core`
- Tested commit: `03e6b0d`
- Runtime: OpenJDK 17.0.20
- Result: PASS

## Command

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
  mvn -B clean \
  -pl renovatio-cobol-runtime,renovatio-cobol-ir,cobol-openrewrite-recipes,renovatio-provider-cobol \
  -am test
```

## Results

| Reactor project | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Renovatio Shared | 23 | 0 | 0 | 0 |
| Renovatio Core | 31 | 0 | 0 | 0 |
| Renovatio Java Provider | 13 | 0 | 0 | 0 |
| Renovatio COBOL Runtime | 23 | 0 | 0 | 0 |
| Renovatio COBOL Intermediate Representation | 55 | 0 | 0 | 0 |
| Renovatio COBOL OpenRewrite Recipes | 3 | 0 | 0 | 0 |
| Renovatio COBOL Provider | 48 | 0 | 0 | 0 |
| **Total** | **196** | **0** | **0** | **0** |

Maven reported `BUILD SUCCESS` after a clean Java 17 compilation. The initial two invocations ran
zero tests because stale Java 21 build output was unreadable by the required Java 17 fork; neither
launch is treated as product evidence. `mvn clean` removed only reproducible `target/` outputs, and
the successful invocation rebuilt all selected modules and dependencies with `release 17`.
