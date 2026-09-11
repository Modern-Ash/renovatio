# Issue 231 Test Report

## Commands

```bash
mvn -pl renovatio-shared,renovatio-mcp-server -am -Dtest=WorkspaceRootPolicyTest,McpToolingServiceCapabilityTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: passed.

```bash
mvn -pl renovatio-api -am -Dexec.skip=true -Dtest=ProjectServiceWorkspaceSecurityTest,WorkbenchProjectAdapterServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: passed.

## Coverage

- Traversal and absolute path rejection.
- Symlink workspace boundary rejection.
- Existing file resolution inside workspace.
- API project creation restricted to configured roots.
- Workbench legacy assets remain read-only.
- Workbench generated targets require development write mode.
- MCP file tools reject absolute and traversal escapes.
