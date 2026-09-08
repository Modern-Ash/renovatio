# Renovatio Workbench — Theia web distribution

Web-first Eclipse Theia application for the Renovatio Workbench track. It provides the IDE shell,
project navigator, governed modernization panels and dashboard continuity without replacing the
existing React dashboard or changing its Spring Boot ownership boundary.

## Runtime

- Eclipse Theia: `1.75.0` (all direct Theia dependencies are exact).
- Node: `24.20.0` (`v1.75.0` requires Node 24 or newer; this spike pins one version).
- Package manager: npm `11.19.0` with a committed lockfile; CI uses `npm ci`.

## Local execution

```bash
nvm use
npm ci
npm run build
npm test
npm run start
```

Open <http://127.0.0.1:3000/>. The Renovatio overview opens at startup and is also available from
the command palette as `Renovatio: Open Workbench Overview` and from the `Renovatio` menu.

Run the automated server check after building:

```bash
npm run smoke
```

Run the Playwright product e2e after building and installing a Chromium browser:

```bash
npm run e2e:install
npm run e2e
```

The e2e test starts the built Theia browser distribution, mocks the governed workbench API with a
versioned COBOL modernization fixture, and verifies the path that motivated the refactor:
COBOL source scan → DomainModel review → architecture profile → shadow impact → approved target
change set → equivalence evidence. The test fails if the target apply action is reachable before the
diff and manifest approval step.

## Install from zero

From a clean clone:

```bash
cd renovatio-workbench
nvm install
nvm use
npm ci
npm run build
npm test
npm run hardening:audit
npm run pilot:demo
npm run smoke
npm run e2e:install
npm run e2e
npm run performance:budget
```

Keep the existing dashboard deployed as before and set `RENOVATIO_DASHBOARD_URL` to its URL. Theia
opens a labelled dashboard link; it does not replace the previous dashboard.

## Docker

```bash
docker build -t renovatio-workbench:release .
docker run --rm -p 3000:3000 -v "$PWD/..:/workspace-data:ro" renovatio-workbench:release
curl --fail --silent --show-error http://127.0.0.1:3000/
```

The workspace mount is read-only by default. A production deployment must use a tenant-aware
workspace service rather than exposing an arbitrary host directory. The runtime stage runs as the
non-root `node` user and prunes development dependencies before distribution.

## Environment contract

| Variable | Default | Purpose |
| --- | --- | --- |
| `RENOVATIO_BACKEND_URL` | `http://127.0.0.1:8080` | Existing Spring Boot API base URL. |
| `RENOVATIO_DASHBOARD_URL` | `http://127.0.0.1:5173/` | Existing administrative dashboard URL, opened as an external labelled link. |
| `RENOVATIO_AUTH_MODE` | `existing-backend` | Names the existing auth adapter; this spike adds no auth protocol. |
| `RENOVATIO_TELEMETRY_ENABLED` | `false` | Explicit opt-in signal; Theia telemetry preference is also off. |
| `RENOVATIO_WORKSPACE_ROOT` | `/workspace` | Displayed workspace policy/root. |

For the temporary unauthenticated development adapter, configure the Spring Boot process — not
the browser bundle — with all of the following explicit settings:

```bash
RENOVATIO_WORKBENCH_DEV_NO_AUTH_ENABLED=true \
RENOVATIO_WORKBENCH_DEV_WRITE_ENABLED=true \
RENOVATIO_WORKBENCH_ALLOWED_ORIGIN=http://127.0.0.1:3000 \
mvn -pl renovatio-api spring-boot:run
```

The `allowed-origin` value must exactly match the browser port. This mode is for a local,
configured workspace only; it must remain disabled for release and production environments.

Do not place tokens, passwords, or private keys in these values or in frontend bundles. Browser
deployments should terminate authentication and TLS at the approved gateway and forward the
existing Renovatio identity/session contract.

## Open VSX and extensions

Open VSX compatibility was evaluated but is deliberately disabled in this prototype. In Theia
`1.75.0`, `@theia/vsx-registry` pulls `@theia/plugin-ext-vscode` and `decompress@4.2.1`; the npm
audit reports critical archive traversal/hardlink advisories with no compatible fixed release.
`config/ovsx-router-config.json` records the intended public registry route, but the package is not
loaded at runtime until the dependency is fixed or an approved sandboxing control is demonstrated.
The same package remains development-only through `@theia/cli`; Docker confines it to the build
stage and `npm prune --omit=dev` excludes it from the runtime graph.

Once unblocked, production must still use a reviewed allowlist, record each extension/version and
license, and reject proprietary Marketplace-only dependencies. The spike requires no third-party
VSIX and does not claim universal VS Code extension compatibility.

## CSP, telemetry, and trust

- Telemetry defaults to off. Enabling it requires a documented data inventory and consent policy.
- Workspace trust is enabled and trash is disabled. The Docker example mounts the workspace read-only.
- The development server may require `unsafe-eval` for bundled code. Production CSP must be measured
  against the generated bundle and enforced at the gateway; no exception is silently claimed safe.
- Commands and extensions run with IDE process privileges. Production requires an extension and
  command allowlist, isolated workspaces, resource limits, and audit logging.

## Security audit gate

Release hardening is encoded in `config/release-hardening.json` and checked by:

```bash
npm run hardening:audit
npm audit --omit=dev --audit-level=high
```

The audit validates Node/npm/Theia compatibility pins, Docker non-root runtime, telemetry defaults,
workspace safety preferences, the Renovatio command allowlist, MCP allowlist status, required docs,
and immutable demo pilot fixture hashes.

## Performance budgets

Continuous-use budgets are versioned in `config/release-hardening.json`:

| Metric | Budget |
| --- | --- |
| Workbench open p95 | `<= 5000 ms` |
| Smoke startup | `<= 90000 ms` |
| Browser RSS | `<= 768 MB` |
| Frontend JS bundle | `<= 32768 KiB` |

Run `npm run smoke` first to emit `.theia-smoke-metrics.json`, then run
`npm run performance:budget` to enforce measured startup, open-p95, RSS, bundle and configured
runtime thresholds.

## Pilot and migration

`npm run pilot:demo` validates the COBOL demo programs and IR fixtures listed in
`config/release-hardening.json`. The pilot report lives in `docs/demo-pilot-report.md`.

For user migration and operational continuation, use:

- `docs/security-and-operations-runbook.md`
- `docs/llm-handoff-guide.md`
- `docs/compatibility-risk-matrix.md`

## Desktop evaluation

Theia supports an Electron target, but this increment does not ship a desktop distribution.
Desktop viability is evaluated in `docs/compatibility-risk-matrix.md`; signing, notarization,
auto-update, sandboxing, and managed deployment remain release work.

## Independent LLM continuation guide

Another LLM can continue from `docs/llm-handoff-guide.md` with the issue context, safety boundaries,
verification commands and known release debt.

## Verification status

Linux build, contract tests, runtime HTTP smoke, Playwright product e2e, hardening audit,
performance budget, demo pilot and production dependency audit are enforced by
`.github/workflows/theia-platform-spike.yml`.
