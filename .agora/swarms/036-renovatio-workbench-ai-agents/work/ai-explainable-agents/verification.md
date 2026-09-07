# Verification

Executed on 2026-09-07 from `/tmp/renovatio-issue181.areLHp`.

- `git diff --check` — passed.
- `npm test --workspace @renovatio/core-ui` — passed.
- `npm run build --workspace @renovatio/core-ui` — passed.
- `mvn -q -pl renovatio-api -am -Dtest=ArchitecturePreviewApiTest,WorkbenchDomainModelApiTest,DecisionLayerApiTest,WorkbenchArchitectureCanvasServiceTest,WorkbenchDomainModelServiceTest,WorkbenchSourceExplorerServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -Dexec.skip=true` — passed.

Coverage added:

- Backend API test for governed AI agents, prompt versions, slash command metadata, canonical context, and human-review policy.
- Frontend contract test for the governed AI panel, dynamic rendering, audit/policy visibility, permission state, and absence of mutating AI methods.
