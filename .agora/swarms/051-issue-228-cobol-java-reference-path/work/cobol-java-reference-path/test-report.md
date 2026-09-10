# Test report

Evidence commands:

```text
./mvnw -q -pl renovatio-provider-cobol -am -Dtest=PipelineE2ETest#shouldExecuteProductionPipelineForAllFixtures+shouldValidateProductionDeterminismAndIdempotency -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true -Dexec.skip=true test
/usr/bin/time -p ./mvnw -q -pl renovatio-provider-cobol -am -Djacoco.skip=true -Dexec.skip=true test
```

Both commands passed. The provider reactor completed 149 tests; `PipelineE2ETest` covers 12 tests, including production execution, determinism, idempotency, stale-source rejection, and semantic-gap blocking behavior. The measured full reactor duration was 88.62 seconds on JDK 21.0.12.
