# Issue 231 Implementation Plan

## Scope

Implement the high-risk local filesystem boundary for issue #231 and document remaining remote-auth debt.

## Steps

1. Add a shared `WorkspaceRootPolicy` for canonical root enforcement.
2. Wire the policy into API project creation, Workbench reads/writes/lists, and MCP file tools.
3. Configure safe defaults in API configuration.
4. Add negative tests for traversal, absolute paths, symlink boundaries, and MCP escapes.
5. Capture security report, operations guide, and test evidence.

## Out Of Scope

- Full OIDC login and remote identity integration.
- Replacing every `X-Role` controller header with a production principal.
- Full SAST/dependency/secret scan automation.

These are deliberately left as technical debt because the current cycle focuses on fail-closed local operation and workspace isolation.
