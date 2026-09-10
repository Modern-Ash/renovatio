# Verification

Commands run from `/tmp/renovatio-issue181.areLHp`:

- `mvn -q -pl renovatio-api -am -Dtest=WorkbenchEquivalenceLabApiTest,WorkbenchEquivalenceServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -Dexec.skip=true`
- `npm test --workspace @renovatio/core-ui` from `renovatio-workbench`
- `npm run build --workspace @renovatio/core-ui` from `renovatio-workbench`
- `mvn -q -pl renovatio-api -am -Dtest=ArchitecturePreviewApiTest,WorkbenchChangeSetApiTest,WorkbenchEquivalenceLabApiTest,WorkbenchEquivalenceServiceTest,WorkbenchDomainModelApiTest,DecisionLayerApiTest,WorkbenchArchitectureCanvasServiceTest,WorkbenchDomainModelServiceTest,WorkbenchSourceExplorerServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -Dexec.skip=true`
- `git diff --check`

All commands completed successfully. The broad backend suite emitted existing ANTLR version warnings but exited 0.
