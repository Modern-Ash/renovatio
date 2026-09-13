# Issue 231 Operations Guide

## Local Default

The API defaults to:

```yaml
server:
  address: 127.0.0.1

renovatio:
  security:
    deployment-mode: local-only
    workspace-roots: ${user.home}/.renovatio/workspaces
```

`local-only` startup requires `server.address` to resolve to a loopback address. `remote` is intentionally fail-closed until authenticated principals replace trusted client role headers.

Keep `renovatio.workbench.dev-no-auth-enabled=false` and `renovatio.workbench.dev-write-enabled=false` unless running a trusted local development session.

## Workspace Roots

Set `renovatio.security.workspace-roots` to a comma-separated allowlist when multiple local roots are needed. Avoid broad roots such as `/`, `/tmp`, or a home directory when the API is reachable by other users.

## Remote Operation

Remote operation is not enabled by this cycle. Before exposing the API or MCP to a network boundary:

- configure OIDC-backed authentication;
- remove trust in client-controlled role headers;
- change `renovatio.security.deployment-mode` only after the remote auth gate is implemented;
- validate CORS, CSRF, and security headers;
- run the negative path and authz tests against the remote profile;
- verify logs do not contain source files, credentials, or raw request bodies.
