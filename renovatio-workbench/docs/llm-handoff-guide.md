# Independent LLM continuation guide

This guide lets another LLM continue Renovatio Workbench work without relying on hidden chat state.

## Context to preserve

- Parent epic: GitHub #175.
- Completed Theia issues: #176 through #184.
- Current hardening issue: #185.
- Workbench root: `renovatio-workbench`.
- Existing dashboard boundary: the legacy dashboard is not replaced; the Theia UI links to it through `RENOVATIO_DASHBOARD_URL`.
- Governed backend contract: Spring Boot endpoints under `/api/projects/{projectId}/workbench/**`.
- Agora records live under `.agora/`; use `agora work inspect` rather than free-reading ledgers.

## Verification commands

From `renovatio-workbench`:

```bash
npm ci
npm run build
npm test
npm run hardening:audit
npm run pilot:demo
npm run smoke
npm run performance:budget
npm audit --omit=dev --audit-level=high
```

From the repository root:

```bash
mvn -q -pl renovatio-api -am -Dtest=ArchitecturePreviewApiTest,WorkbenchChangeSetApiTest,WorkbenchEquivalenceLabApiTest,WorkbenchEquivalenceServiceTest,WorkbenchDomainModelApiTest,DecisionLayerApiTest,WorkbenchArchitectureCanvasServiceTest,WorkbenchDomainModelServiceTest,WorkbenchSourceExplorerServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -Dexec.skip=true
git diff --check
```

## Known release debt

- Desktop packaging is feasible but not shipped; signing, notarization, update channels, and sandboxing need a dedicated release task.
- Open VSX remains disabled until the Theia plugin dependency line has no high/critical production risk or an approved sandbox.
- Production CSP must be measured against the final hosted bundle and enforced at the gateway.
- Telemetry stays opt-in and requires a data inventory before being enabled.
- Global Agora ledger validation is tracked separately in #188.

## Safe next changes

When extending the workbench, prefer adding a backend contract test, a UI contract test, and one release-hardening assertion in the same PR. Keep writes behind governed change sets unless a future issue explicitly expands workspace write policy.
