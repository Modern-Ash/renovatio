# Verification report

| Check | Result | Evidence |
| --- | --- | --- |
| Shared equivalence unit tests | success | `mvn -q -pl renovatio-shared test -Dexec.skip=true` |
| COBOL characterization contract | success | `mvn -q -pl renovatio-provider-cobol clean test -Dtest=CharacterizationFixtureContractTest -Dexec.skip=true` |
| COBOL/Java golden output | success | `equivalence-balance` produces `0950` in both runtimes; SHA-256 `1dd0e793219cf23e966b1f6260c202d3db57cf46a1c5e48abe49b954738bca51` |
| API equivalence adapter test | success | `WorkbenchEquivalenceServiceTest` |
| API package | success | `mvn -q -pl renovatio-api -am -Dexec.skip=true -DskipTests package` |
| Workbench contract tests | success | `npm test` in `renovatio-workbench` |
| SQLite workbench endpoint | success | local API returned two migrated projects from `/api/workbench/projects` |
| Production shadow/canary/rollback drill | deferred | no approved operational environment exists |

The deferred operational item is an explicit scope boundary, not a release approval. The local gate
blocks `REGRESSION` and `UNDETERMINED` reports.
