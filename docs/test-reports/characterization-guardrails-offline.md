# Characterization Guardrails Offline Verification — 2026-08-31

## Environment

- Image: `maven:3.9.12-eclipse-temurin-17`
- Digest: `sha256:a0603aab698040d9c94259f379ec0487da1678560748d6c7508483034033c53d`
- Network: Docker `--network=none`
- Provider credentials: absent

## Procedure

The connected preparation pass ran `dependency:go-offline test` to resolve Maven's effective test
classpath, including dynamically selected Surefire providers. The verification pass then ran:

```bash
mvn -B -o -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am test
```

inside the same image and cache with networking disabled. DNS lookup and a direct TCP connection
were both verified unavailable before Maven started.

## Result

- Build: SUCCESS
- Tests: 201 passed, 0 failures, 0 errors, 0 skipped
- Offline dependency resolution: PASS
- Provider credential absence: PASS
- Network isolation: PASS

Two preliminary offline attempts correctly failed because Maven's `dependency:go-offline` alone,
and then a `-DskipTests` preparation, omitted effective test dependencies. The committed workflow
uses the successful full preparation pass and therefore reproduces the green isolated run.

