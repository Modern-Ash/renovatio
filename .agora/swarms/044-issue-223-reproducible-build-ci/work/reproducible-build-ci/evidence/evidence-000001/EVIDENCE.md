# Evidence: Maven Reactor Build

- **Type:** build
- **Result:** success
- **Date:** 2026-09-08
- **Command:** `./mvnw clean install -Djacoco.skip=true`
- **Environment:** Ubuntu 24.04, Java 21.0.12, Maven 3.9.6 (Wrapper)
- **Commit:** (branch: agora/issue-223-reproducible-build-ci)

## Result

All 22 modules compiled and all tests passed:

| Module | Status |
|--------|--------|
| Renovatio Parent | SUCCESS |
| renovatio-semantic-ir | SUCCESS |
| renovatio-domain-model | SUCCESS |
| renovatio-profile | SUCCESS |
| renovatio-decisions | SUCCESS |
| renovatio-shared | SUCCESS |
| renovatio-cobol-runtime | SUCCESS |
| renovatio-cobol-ir | SUCCESS |
| renovatio-llm | SUCCESS |
| renovatio-jcl | SUCCESS |
| renovatio-architecture | SUCCESS |
| renovatio-core | SUCCESS |
| renovatio-provider-java | SUCCESS |
| renovatio-cobol-annotations | SUCCESS |
| cobol-openrewrite-recipes | SUCCESS |
| renovatio-provider-cobol | SUCCESS |
| renovatio-mcp-server | SUCCESS |
| renovatio-persistence | SUCCESS |
| renovatio-emitter-node | SUCCESS |
| renovatio-cli | SUCCESS |
| renovatio-api | SUCCESS |
| renovatio-evals | SUCCESS |

## Changes made

- Installed Maven Wrapper 3.9.6
- Centralized JaCoCo in root pluginManagement
- Removed 2 duplicate renovatio-mcp-server entries from dependencyManagement
- Added renovatio-cli, renovatio-cobol-runtime, renovatio-evals to dependencyManagement
- Fixed hardcoded versions in renovatio-provider-java (OpenRewrite, JGit)
- Removed hardcoded version in renovatio-cobol-ir for renovatio-cobol-runtime
- Removed redundant ${project.version} in renovatio-persistence
- Removed redundant version in renovatio-shared maven-compiler-plugin
- Fixed renovatio-emitter-node JaCoCo: added version, moved check to execution
- Removed hardcoded JaCoCo versions from 8 child modules
- Unified Java version to 21 (removed 17 overrides from shared, core, provider-java)
