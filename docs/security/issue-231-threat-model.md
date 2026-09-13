# Issue 231 Threat Model

| Threat | Surface | Risk | Mitigation |
| --- | --- | --- | --- |
| Path traversal reads host files | API Workbench, MCP file tools | High | `WorkspaceRootPolicy.resolveExisting` rejects absolute paths and normalized escapes. |
| Path traversal writes host files | API Workbench, MCP file tools | High | `resolveForWrite` rejects absolute paths and normalized escapes before writing. |
| Symlink escape | API Workbench, MCP file tools | High | Workspace boundaries, write targets, and existing read targets reject symlinks. |
| Client-controlled role spoofing | API controllers | High for remote | Default mode remains local-only; remote OIDC/principal mapping is follow-up technical debt. |
| Accidental source or secret disclosure | Logs/reports | Medium | Reports and tests record command outcomes, not file contents or secrets. |
| Unsafe generated asset overwrite | Workbench writes | Medium | Writes require dev mode and target generated-language extensions. |
| Overbroad workspace creation | Project creation | High | New projects must resolve under configured `renovatio.security.workspace-roots`. |

## Residual Risk

Remote exposure is not approved by this change. A remote deployment must first replace `X-Role` trust with a server-authenticated principal and validate HTTP security headers, CSRF posture, and OIDC failure modes.
