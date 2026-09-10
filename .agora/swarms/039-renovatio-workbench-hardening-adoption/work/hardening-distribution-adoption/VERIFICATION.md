# Verification

Commands run:

- `npm run build` from `renovatio-workbench` — success.
- `npm run hardening:audit` from `renovatio-workbench` — success.
- `RENOVATIO_PERFORMANCE_ACCEPT_STATIC=1 npm run performance:budget` from `renovatio-workbench` — success for bundle/static budgets on the local Node 20 host.
- `npm run pilot:demo` from `renovatio-workbench` — success.
- `npm test` from `renovatio-workbench` — success.
- `mvn -q -pl renovatio-api -am -Dtest=ArchitecturePreviewApiTest,WorkbenchChangeSetApiTest,WorkbenchEquivalenceLabApiTest,WorkbenchEquivalenceServiceTest,WorkbenchDomainModelApiTest,DecisionLayerApiTest,WorkbenchArchitectureCanvasServiceTest,WorkbenchDomainModelServiceTest,WorkbenchSourceExplorerServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -Dexec.skip=true` — success.
- `npm audit --omit=dev --audit-level=high` from `renovatio-workbench` with network access — success; only moderate Theia-line advisories remain.
- `git diff --check` — success.

Local `npm run smoke` was not counted as passing because this host has Node `v20.19.0` while the workbench is pinned to Node `24.20.0`; the job is enforced in GitHub Actions with Node `24.20.0` and now emits measured startup/open-p95/RSS metrics consumed by `npm run performance:budget`. Review feedback removed workflow path filters so backend contract changes always trigger the release gate.
