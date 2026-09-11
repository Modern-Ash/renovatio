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

```bash
mvn -pl renovatio-api -am -Dexec.skip=true -Dtest=WorkbenchChangeSetApiTest,WorkbenchEquivalenceLabApiTest,WorkspaceRootPolicyTest,ProjectServiceWorkspaceSecurityTest,WorkbenchProjectAdapterServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: passed.

```bash
mvn -q -pl renovatio-api -am -Dtest=ArchitecturePreviewApiTest,WorkbenchChangeSetApiTest,WorkbenchEquivalenceLabApiTest,WorkbenchEquivalenceServiceTest,WorkbenchDomainModelApiTest,DecisionLayerApiTest,WorkbenchArchitectureCanvasServiceTest,WorkbenchDomainModelServiceTest,WorkbenchSourceExplorerServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -Dexec.skip=true
```

Result: passed.

## Coverage

- Traversal and absolute path rejection.
- Symlink workspace boundary rejection, including intermediate symlink components.
- Existing file resolution inside workspace.
- Outside workspace creation rejected before creating parent directories.
- API project creation restricted to configured roots.
- Workbench legacy assets remain read-only.
- Workbench generated targets require development write mode.
- MCP file tools reject absolute and traversal escapes.
- API workbench contract tests allow test `@TempDir` roots while production remains restricted.
