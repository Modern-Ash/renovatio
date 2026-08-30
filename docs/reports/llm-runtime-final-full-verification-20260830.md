# LLM Runtime Final Full Verification

- Date: 2026-08-30
- Tested commit: `354d3cb4e12400164f5a24144fd7d674483a8165`
- Command: `env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/bin:/usr/bin:/bin mvn test -q`
- Result: `BUILD SUCCESS` (exit code 0)
- Reactor modules: 10/10 successful
- Tests: 254 run, 254 passed, 0 failures, 0 errors, 0 skipped

This commit-bound verification includes the strict versioned-schema and malformed-catalog rejection
coverage, exact Anthropic request-body policy with numeric temperature zero, the governed A/B/C/D
promotion lifecycle, catalog-owned deterministic fallbacks, and all prior Renovatio modules.

No live LLM or network provider was used. Optional MCP tests attempted unavailable loopback services
and remained outside the deterministic test count; the MCP module's deterministic suite passed.
