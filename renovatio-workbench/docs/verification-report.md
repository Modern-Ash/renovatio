# Theia workbench verification report

- Date: 2026-09-07
- Evaluated revision: Theia workbench release-readiness track through issue #185
- Host executor: Linux container runtime
- Runtime pins: Eclipse Theia `1.75.0`, Node `24.20.0`, npm `11.19.0`

## Automated evidence

| Check | Command | Result |
| --- | --- | --- |
| Reproducible install | `npm ci` in `node:24.20.0-bookworm-slim` | Pass; committed lockfile accepted |
| Production bundle | `npm run build` | Pass; browser and node bundles report zero errors |
| Reproducible Docker build stage | `docker build --target build -t renovatio-workbench:ci-build .` | Enforced in CI |
| Contract tests | `npm test` | Pass; core UI contract tests cover Theia areas through Equivalence Lab |
| API contract regressions | `mvn -q -pl renovatio-api -am -Dtest=ArchitecturePreviewApiTest,WorkbenchChangeSetApiTest,WorkbenchEquivalenceLabApiTest,WorkbenchEquivalenceServiceTest,WorkbenchDomainModelApiTest,DecisionLayerApiTest,WorkbenchArchitectureCanvasServiceTest,WorkbenchDomainModelServiceTest,WorkbenchSourceExplorerServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -Dexec.skip=true` | Pass locally and enforced in CI |
| Hardening audit | `npm run hardening:audit` | Pass; compatibility pins, Docker runtime, telemetry, workspace safety, allowlists and docs verified |
| Performance budgets | `npm run smoke && npm run performance:budget` | Enforced in CI with measured startup, open-p95, RSS and frontend bundle budgets |
| Demo pilot | `npm run pilot:demo` | Pass; COBOL demo and IR fixture hashes match |
| Runtime dependency audit | `npm audit --omit=dev --json` after production prune | 0 critical, 0 high, 22 moderate |
| HTTP startup | Runtime container plus `curl --fail http://127.0.0.1:30176/` | Pass; HTTP 200 and `Renovatio Workbench` HTML shell |

The audit result is an improvement over the Open VSX-enabled prototype, whose production graph
contained a critical `decompress@4.2.1` path. Plugin and Open VSX packages are excluded from this
runtime. The remaining 22 moderate findings are inherited from the pinned Theia graph and are
recorded in the compatibility matrix; CI rejects new high or critical production findings.

The development graph still contains `decompress@4.2.1` through `@theia/cli`. It is used only in the
isolated build stage, is removed by `npm prune --omit=dev`, and is not copied into the runtime image
as a production dependency. This containment is acceptable for a non-production spike, not a final
supply-chain approval; a compatible upstream fix or replacement remains required before release.

## Behavior covered by the automated checks

- The application registers command `renovatio.workbench.open` and a Renovatio menu action.
- The custom `ReactWidget` is included in a clean production bundle and opened by the frontend
  contribution during startup.
- The widget exposes the backend URL, auth mode, workspace root and telemetry environment contract.
- Focus indication, narrow-layout behavior and reduced-motion behavior have source-level tests.

These checks establish bundle inclusion and server readiness. They do not substitute for a visual
browser assertion.

## Remaining release gates

| Evidence | Status | Why it remains open |
| --- | --- | --- |
| Desktop/Electron package | Optional | Signing, notarization, auto-update and sandboxing are explicitly out of scope for the web distribution |
| Open VSX enablement | Blocked | Plugin dependency risk remains documented in the compatibility matrix |
| Production CSP measurement | Required before hosted production | Gateway must measure the final generated bundle and enforce the recorded baseline |
