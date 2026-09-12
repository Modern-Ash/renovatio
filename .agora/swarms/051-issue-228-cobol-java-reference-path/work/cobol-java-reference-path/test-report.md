# Test report

Evidence commands:

```text
./mvnw -q -pl renovatio-provider-cobol -am -Dtest=PipelineE2ETest#shouldExecuteProductionPipelineForAllFixtures+shouldValidateProductionDeterminismAndIdempotency -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true -Dexec.skip=true test
/usr/bin/time -p ./mvnw -q -pl renovatio-provider-cobol -am -Djacoco.skip=true -Dexec.skip=true test
./mvnw -q -pl renovatio-provider-cobol,renovatio-cli,renovatio-api -am -Dtest=PipelineE2ETest,PipelineValidationTest,SurfaceProofTest,FixturesExistenceTest,RenovatioCliSmokeTest,ReferencePipelineApiTest -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true -Dexec.skip=true test
./mvnw -q -pl renovatio-cli -am -DskipTests -Djacoco.skip=true -Dexec.skip=true package
java -jar renovatio-cli/target/renovatio.jar reference-pipeline renovatio-provider-cobol/src/test/resources/fixtures/batch-simple --out /tmp/renovatio-reference-cli/batch-simple-final
```

The provider commands passed. The provider reactor completed 149 tests; `PipelineE2ETest` covers 12 tests, including production execution, determinism, idempotency, stale-source rejection, and semantic-gap blocking behavior. The measured full reactor duration was 88.62 seconds on JDK 21.0.12. The cross-surface command additionally validates CLI registration, the REST API endpoint, MCP/service surface proof, fixture checks, and pipeline validators. The packaged CLI command completed the reference fixture with 9/9 stages, zero blocking semantic gaps, and passing equivalence.
