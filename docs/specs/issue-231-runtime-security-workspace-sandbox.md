# Issue 231 Runtime Security and Workspace Sandbox

Issue #231 hardens API, MCP, and Workbench file boundaries for safe local operation. The default deployment mode is `local-only`; remote operation remains explicit and must fail closed until a real identity provider is configured.

## Deployment Modes

- `local-only` is the default mode.
- Workspace access is constrained to configured roots from `renovatio.security.workspace-roots`.
- Remote mode must not rely on client-controlled role headers for authentication. OIDC remains the planned remote identity boundary.

## Workspace Sandbox

All API and MCP filesystem access must pass through `WorkspaceRootPolicy`.

- Workspace roots are canonical directories.
- Relative workspaces are resolved under the configured root.
- Absolute workspace paths are accepted only when they remain under an allowed root.
- Asset access rejects absolute paths, traversal, symlink boundaries, and symlink file targets.
- Write paths create parent directories only after caller permissions and target type are validated.

## Safe Apply

Workbench write support remains limited to generated target extensions and requires development write mode. Legacy COBOL, copybook, and JCL assets remain read-only through Workbench.

## Web Hardening

The API configuration documents local-only defaults, disabled no-auth mode, bounded multipart request size, and explicit origin configuration. Additional remote-facing HTTP hardening is tracked as follow-up debt with OIDC.

## Auditability

This change avoids logging file contents, source snippets, or secrets. Evidence is captured in the issue security report and test report.
