# LLM runtime full-reactor verification

Date: 2026-08-30

## Result

The Java 17 Maven reactor completed successfully after the issue 125 rework.

- Modules: 10 successful, 0 failed
- Tests: 245 passed, 0 failed, 0 errors, 0 skipped
- Command: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn test`
- Duration: 58.394 seconds

## Relevant coverage

The run includes 27 `renovatio-llm` tests covering the prompt catalog, strict output validation,
provider runtime, committed-cache verification, governed attribution and executable CLI. Its
dependency modules cover canonical annotated-IR identity and the renamed COBOL NQL parsing service.

The MCP integration probe attempted unavailable local endpoints and reported zero probe tests, as in
the pre-existing suite behavior. The remaining 22 MCP tests passed, and the module completed
successfully.

## Governed runtime check

The separate governed offline execution `tool-20260830t19141788128053z` completed successfully and
is documented in `llm-runtime-catalog-cache-rework-report.md`.
