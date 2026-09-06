# Issue 176 verification report

- Date: 2026-09-05
- Evaluated revision: working tree on `agora/renovatio-workbench`
- Host executor: Linux container runtime
- Runtime pins: Eclipse Theia `1.75.0`, Node `24.20.0`, npm `11.19.0`

## Automated evidence

| Check | Command | Result |
| --- | --- | --- |
| Reproducible install | `npm ci` in `node:24.20.0-bookworm-slim` | Pass; committed lockfile accepted |
| Production bundle | `docker build --target build -t renovatio-workbench:issue-176-build .` | Pass; browser and node bundles report zero errors |
| Contract tests | `docker run --rm --entrypoint npm renovatio-workbench:issue-176-build test` | Pass; 3 tests, 0 failures |
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

## Pending platform evidence

| Evidence | Status | Why it remains open |
| --- | --- | --- |
| Browser capture showing the custom widget | Pending | The in-app browser runtime reported that no browser instance was available |
| Command palette/menu interaction | Pending | Requires the same interactive browser executor |
| macOS browser launch and capture | Pending | No macOS executor is available in the current environment |
| Desktop/Electron package | Not required for the spike decision | Feasibility only; release engineering is separately gated |

Issue #176 must remain in verification until the required browser and macOS evidence is attached
and independently reviewed. Issue #177 must remain blocked on #176 meanwhile.
