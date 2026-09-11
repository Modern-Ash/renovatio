# Issue 233 Test Report

Status: PASS.

Command:

```bash
mvn -q -pl renovatio-decisions,renovatio-llm,renovatio-jcl,renovatio-application -am \
  -Dexec.skip=true \
  -Dtest=DecisionSuggestionServiceTest,BatchDecisionPointsTest,F7AcceptanceTest,ReviewRegressionTest,DefaultRenovatioApplicationTest,ApplicationArchitectureTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: PASS.

Coverage:

- `DecisionSuggestionServiceTest`: LLM runtime still implements the shared decisions-domain suggestion contract.
- `BatchDecisionPointsTest`: JCL batch decisions use the shared prompt identity and no longer import the LLM runtime.
- `F7AcceptanceTest` and `ReviewRegressionTest`: existing parser/projection/emitter behavior remains green.
- `DefaultRenovatioApplicationTest`: preview combines target artifacts and batch orchestration through one application manifest.
- `ApplicationArchitectureTest`: application module remains transport/framework/filesystem neutral.

Additional check:

```bash
git diff --check
```

Result: PASS.

Dependency isolation check:

```bash
mvn -q -pl renovatio-jcl dependency:tree -Dincludes=org.shark.renovatio:renovatio-llm
```

Result: PASS. The filtered dependency tree produced no entries for
`renovatio-llm` under `renovatio-jcl`.
