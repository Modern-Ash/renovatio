# Deterministic Recipe Boundary Review Revalidation — 2026-08-31

## Review finding

PR #137 correctly identified that the original boundary test inspected only `src/main/java` and a
small raw-POM blacklist. It could miss prompt or credential resources and transitive HTTP clients.

## Correction

- The architecture test now inspects paths and textual content for every production entry under
  `src/main`, including `src/main/resources`.
- Maven Enforcer runs in `validate` and rejects direct or transitive HTTP-client and LLM-provider
  dependencies before compilation.
- The byte-stability assertion remains unchanged.

## Verification

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
  mvn -B -pl cobol-openrewrite-recipes -am test
```

- Maven Enforcer `BannedDependencies`: PASS
- Reactor tests: 106 passed, 0 failures, 0 errors, 0 skipped
- Build result: SUCCESS

