# Post-promotion full-reactor verification

Date: 2026-08-30

The Java 17 Maven reactor completed successfully after cache promotion and the governed committed
cache-hit check.

- Tested commit: `1401309a77b473dd15836d10c9a595b7f3f73ce3`
- Modules: 10 successful, 0 failed
- Tests: 248 passed, 0 failed, 0 errors, 0 skipped
- Command: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn test`

As in earlier runs, the MCP probe could not connect to optional local endpoints in the sandbox and
reported zero probe cases; its remaining 22 tests and module build passed.
