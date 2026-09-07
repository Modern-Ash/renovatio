# Security and operations runbook

This runbook is the release-readiness gate for operating Renovatio Workbench continuously without breaking the existing dashboard or backend contracts.

## Workspace isolation

- Run the web distribution with an explicit workspace root: `/workspace-data`.
- Mount pilot and production workspaces read-only unless a governed change-set flow explicitly needs writes.
- Do not expose arbitrary host paths to Theia. Tenant workspaces must be provisioned by an approved workspace service.
- Keep `files.enableTrash=false` so destructive IDE actions do not create hidden recoverability assumptions.

## Command allowlist

Only commands listed in `config/release-hardening.json` are approved for the Renovatio extension:

- `renovatio.workbench.open`
- `renovatio.shell.project`
- `renovatio.shell.analysis`
- `renovatio.shell.domain`
- `renovatio.shell.architecture`
- `renovatio.shell.shadow`
- `renovatio.shell.ai`
- `renovatio.shell.changes`
- `renovatio.shell.equivalence`

Adding commands requires updating the allowlist and passing `npm run hardening:audit`.

## MCP allowlist

No MCP server is enabled by the workbench distribution. Future MCP adapters must be listed in `config/release-hardening.json`, scoped to project workspaces, audited for secrets handling, and tested in CI before release.

## CSP gateway policy

The production gateway must enforce the CSP baseline recorded in `config/release-hardening.json`:

- `default-src 'self'`
- `connect-src 'self'` plus the configured Renovatio backend/dashboard origins
- `img-src 'self' data:`
- `style-src 'self' 'unsafe-inline'` until Theia theming is separately tightened
- `script-src 'self'`
- `frame-ancestors 'none'`

Development-only `unsafe-eval` must not be silently promoted to production. If a generated Theia bundle requires it, record the exact bundle evidence and block release until the risk is accepted.

## Telemetry opt-in

Telemetry is off by default in both Theia preferences and Docker environment. Enabling telemetry requires:

1. A data inventory.
2. User-visible consent text.
3. Retention and deletion policy.
4. A reviewed destination allowlist.
5. A passing `npm run hardening:audit`.

## Security audit gate

Before release or merge:

```bash
npm ci
npm run build
npm run hardening:audit
npm run smoke
npm run performance:budget
npm audit --omit=dev --audit-level=high
```

Any high or critical production dependency finding blocks release. Moderate Theia-line findings must remain documented in the compatibility matrix.

## Logs and metrics

Operational logs must include:

- startup time until HTTP ready;
- selected project id, never file contents or secrets;
- command id;
- backend request path and status;
- equivalence/change-set run id and report hash.

Minimum metrics:

- workbench open p95;
- smoke startup duration;
- backend adapter error rate;
- browser RSS budget;
- frontend bundle size.
