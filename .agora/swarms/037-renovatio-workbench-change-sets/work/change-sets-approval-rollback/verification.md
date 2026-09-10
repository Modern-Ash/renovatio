# Verification

Executed on 2026-09-07 from `/tmp/renovatio-issue181.areLHp`.

- `mvn -q -pl renovatio-api -am -Dtest=WorkbenchChangeSetApiTest -Dsurefire.failIfNoSpecifiedTests=false test -Dexec.skip=true` — passed.
- `mvn -q -pl renovatio-api -am -Dtest=ArchitecturePreviewApiTest,WorkbenchChangeSetApiTest,WorkbenchDomainModelApiTest,DecisionLayerApiTest,WorkbenchArchitectureCanvasServiceTest,WorkbenchDomainModelServiceTest,WorkbenchSourceExplorerServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -Dexec.skip=true` — passed.
- `npm test --workspace @renovatio/core-ui` — passed.
- `npm run build --workspace @renovatio/core-ui` — passed.
- `git diff --check` — passed.

Coverage confirms state transitions, diff-before-approval, viewer mutation denial, rejected no-op behavior, apply, rollback, manifest/audit history, and frontend contract presence.
