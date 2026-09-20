# Issue 293 VS Code Workbench Documentation Verification

Issue: #293

Parent epic: #281

Branch: `feature/issue-293-vscode-workbench-docs`

## Documentation Updated

- `renovatio-vscode-extension/README.md`
- `renovatio-vscode-extension/schemas/README.md`
- `docs/plans/renovatio-vscode-workbench.md`
- `docs/reports/issue-293-vscode-workbench-docs-verification.md`

## Coverage

- Quick start and evaluator flow.
- Artifact contracts for workspace manifest, migration map, change sets, evidence bundles and diagrams.
- Backend and LLM configuration, health checks, smoke tests and restart safety.
- Two-way navigation from COBOL/JCL to target code and from target code back to legacy source.
- Local-first safety model, no silent overwrite, approval gates, backups and sync conflict states.
- Agent handoff entry points for commands, views, schemas, migration-map types, diagram model behavior, tests and fixtures.

## Verification Commands

The previous ticket branch validated the full codebase and extension package with:

```sh
cd renovatio-vscode-extension
npm test
npm run build
npm run package

cd ..
./mvnw -B verify -Djacoco.skip=true -Dexec.skip=true
(cd renovatio-ui && npm run build)
(cd renovatio-workbench && npm run build)
```

For this documentation-only ticket, rerun at least `npm test` after editing examples or schemas.

## Fresh-Agent Read

The README now names the primary commands and artifact files needed to start work from a fresh clone. The plan document maps those commands back to implementation entry points so future agents can extend the workbench without duplicating abstractions.
