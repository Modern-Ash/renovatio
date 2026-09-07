# Verification

Verified on 2026-09-07 in worktree `/tmp/renovatio-issue180.ZcLGOR`.

## Commands

- `mvn -q -pl renovatio-api -am -Dtest=WorkbenchArchitectureCanvasServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -Dexec.skip=true` — success.
- `mvn -q -pl renovatio-api -am -Dtest=ArchitecturePreviewApiTest,WorkbenchArchitectureCanvasServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -Dexec.skip=true` — success.
- `mvn -q -pl renovatio-profile,renovatio-architecture,renovatio-api -am test -Dexec.skip=true` — success.
- `npm test --workspace @renovatio/core-ui` — success.
- `npm run build --workspace @renovatio/core-ui` — success.
- `npm run build` from `renovatio-workbench` — success; install used `npm_config_engine_strict=false npm ci --ignore-scripts` because the local environment has Node 20.19/npm 10.8 while the workbench declares Node 24.20/npm >= 11 and native `keytar` scripts require unavailable `libsecret`.

## Criteria mapping

- `domain-model-immutable`: `WorkbenchArchitectureCanvasServiceTest.mvcDefaultsExposeModelServiceControllerAndDoNotMutateDomainModel` verifies architecture saves leave domain model revisions unchanged.
- `mvc-defaults`: service and transformer tests verify MVC defaults expose model, service, and controller layers.
- `custom-naming-shadow`: service tests verify custom packages/suffixes recompute preview manifest without generated artifacts.
- `illegal-dependencies-visible`: service and UI contract tests verify forbidden dependency rules surface diagnostics before generation.
- `profile-versioned-hash`: service tests verify canonical hashes, compare, restore-as-new-revision, and stale revision conflict handling.
