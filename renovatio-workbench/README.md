# Renovatio Workbench — Theia platform spike

Minimal web-first Eclipse Theia application for issue #176. It proves a local workspace, file
navigator, custom Renovatio widget, command and menu without replacing the current React dashboard
or changing the Spring Boot backend.

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

## Docker

```bash
docker build -t renovatio-workbench:issue-176 .
docker run --rm -p 3000:3000 -v "$PWD/..:/workspace-data:ro" renovatio-workbench:issue-176
curl --fail --silent --show-error http://127.0.0.1:3000/
```

The workspace mount is read-only in this spike. A production deployment must use a tenant-aware
workspace service rather than exposing an arbitrary host directory.

## Environment contract

| Variable | Default | Purpose |
| --- | --- | --- |
| `RENOVATIO_BACKEND_URL` | `http://127.0.0.1:8080` | Existing Spring Boot API base URL. |
| `RENOVATIO_DASHBOARD_URL` | `http://127.0.0.1:5173/` | Existing administrative dashboard URL, opened as an external labelled link. |
| `RENOVATIO_AUTH_MODE` | `existing-backend` | Names the existing auth adapter; this spike adds no auth protocol. |
| `RENOVATIO_TELEMETRY_ENABLED` | `false` | Explicit opt-in signal; Theia telemetry preference is also off. |
| `RENOVATIO_WORKSPACE_ROOT` | `/workspace` | Displayed workspace policy/root. |

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

## Desktop evaluation

Theia supports an Electron target, but this increment does not ship a desktop distribution.
Desktop viability is evaluated in `docs/compatibility-risk-matrix.md`; signing, notarization,
auto-update, sandboxing, and managed deployment remain release work.

## Verification status

Linux build, contract tests, runtime HTTP smoke and the production dependency audit are documented
in `docs/verification-report.md`. Browser interaction/capture and macOS execution remain required;
the spike and its dependent issue must not be marked complete until that evidence is reviewed.
