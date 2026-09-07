# Verification

Local validation:

- `mvn -q -pl renovatio-api -am -Dtest=ArchitecturePreviewApiTest,WorkbenchArchitectureCanvasServiceTest,WorkbenchDomainModelServiceTest,WorkbenchSourceExplorerServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -Dexec.skip=true` — passed.
- `npm test --workspace @renovatio/core-ui` — passed.
- `npm run build --workspace @renovatio/core-ui` — passed.

Remote validation on PR #192:

- `build` — passed.
- `build-and-smoke` — passed in 1m54s.
- `characterization-offline` — passed.

GitHub closure:

- PR #192 merged at `2026-09-07T18:47:41Z`.
- Issue #181 closed at `2026-09-07T18:47:42Z`.

