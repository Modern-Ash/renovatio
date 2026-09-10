# Implementation Plan: Build reproducible y CI obligatoria (#223)

## Phase 1: Fix Maven Reactor (AC: maven-green, versions-pinned)

### Step 1.1: Add Maven Wrapper
- Run `mvn wrapper:wrapper -Dmaven=3.9.6` to generate `.mvn/wrapper/maven-wrapper.properties` and `mvnw`/`mvnw.cmd`
- Verify `./mvnw --version` outputs 3.9.6
- Commit `.mvn/`, `mvnw`, `mvnw.cmd`
- **Depends on:** nothing
- **ACs covered:** versions-pinned, single-bootstrap

### Step 1.2: Centralize JaCoCo in root pluginManagement
- Add to root `pom.xml` `<pluginManagement>`:
  ```xml
  <plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
  </plugin>
  ```
- Remove `<version>0.8.10</version>` from all 8 child modules that declare it
- Fix `renovatio-emitter-node/pom.xml`: add `<version>0.8.10</version>`, move check rule into `<execution><id>check</id>`
- **Depends on:** 1.1
- **ACs covered:** maven-green, versions-pinned

### Step 1.3: Fix dependencyManagement duplicates and gaps
- Remove 2 duplicate `renovatio-mcp-server` entries (keep first occurrence only)
- Add `renovatio-cli`, `renovatio-cobol-runtime`, `renovatio-evals` to `<dependencyManagement>`
- **Depends on:** 1.1
- **ACs covered:** maven-green

### Step 1.4: Fix hardcoded versions in child modules
- `renovatio-provider-java/pom.xml`: replace hardcoded OpenRewrite versions with `${openrewrite.version}`, replace JGit `6.8.0` with `${jgit.version}`
- `renovatio-cobol-ir/pom.xml`: remove hardcoded `<version>0.0.1-SNAPSHOT</version>` for `renovatio-cobol-runtime` (use managed version)
- `renovatio-persistence/pom.xml`: remove redundant `${project.version}` from managed deps
- `renovatio-provider-java/pom.xml`: remove redundant `${project.version}` from managed deps
- `renovatio-shared/pom.xml`: remove redundant `<version>3.11.0</version>` from maven-compiler-plugin
- **Depends on:** 1.3
- **ACs covered:** maven-green

### Step 1.5: Unify Java version to 21
- Remove `<source>17</source><target>17</target>` overrides from:
  - `renovatio-shared/pom.xml`
  - `renovatio-core/pom.xml`
  - `renovatio-provider-java/pom.xml`
- All modules inherit `21` from root `pluginManagement`
- **Depends on:** 1.1
- **ACs covered:** maven-green, versions-pinned

### Step 1.6: Verify Maven reactor builds green
- Run `./mvnw clean install` from root
- Fix any remaining compilation or test failures
- Capture output as evidence
- **Depends on:** 1.2, 1.3, 1.4, 1.5
- **ACs covered:** maven-green

## Phase 2: Fix Node Build (AC: single-bootstrap, node-collision)

### Step 2.1: Add .nvmrc to renovatio-ui
- Create `renovatio-ui/.nvmrc` with content `24.20.0`
- **Depends on:** nothing
- **ACs covered:** versions-pinned

### Step 2.2: Verify renovatio-ui builds with npm ci
- `cd renovatio-ui && npm ci && npm run build`
- Verify output goes to `renovatio-api/src/main/resources/static/`
- Capture output as evidence
- **Depends on:** nothing
- **ACs covered:** single-bootstrap

### Step 2.3: Verify renovatio-workbench builds with npm ci
- `cd renovatio-workbench && npm ci && npm run build`
- Verify output is correct
- Capture output as evidence
- **Depends on:** nothing
- **ACs covered:** single-bootstrap

### Step 2.4: Fix node-idioms.md collision
- Investigate the emitter-node multiprogram generation for path duplication
- Fix the path generation logic to produce deterministic, non-duplicated paths
- Verify `docs/node-idioms.md` is generated (not committed) and artifact is deterministic
- **Depends on:** 1.6 (needs green Maven build)
- **ACs covered:** node-collision

## Phase 3: Python Build (AC: single-bootstrap)

### Step 3.1: Verify Python builds and tests
- `cd renovatio-provider-python && pip install -e ".[test]" && pytest`
- Verify tests pass
- Capture output as evidence
- **Depends on:** nothing
- **ACs covered:** single-bootstrap

### Step 3.2: Verify migration spec Python tests
- `cd specs/1-cobol-python-migration && bash scripts/setup_env.sh && source .venv/bin/activate && pytest tests/`
- Capture output as evidence
- **Depends on:** nothing
- **ACs covered:** single-bootstrap

## Phase 4: Bootstrap Script (AC: single-bootstrap, developer-parity)

### Step 4.1: Create scripts/bootstrap.sh
- Script that orchestrates the full build:
  1. Check prerequisites (Java 21, Node 24, Python 3.10+)
  2. `./mvnw clean install` (Maven reactor)
  3. `cd renovatio-ui && npm ci && npm run build` (Vite SPA)
  4. `cd renovatio-workbench && npm ci && npm run build` (Theia workbench)
  5. `cd renovatio-provider-python && pip install -e ".[test]" && pytest` (Python)
- Script fails on first error (no `|| true`)
- Script is idempotent (safe to run multiple times)
- **Depends on:** 1.6, 2.2, 2.3, 3.1
- **ACs covered:** single-bootstrap, developer-parity

### Step 4.2: Test bootstrap from clean clone
- Create temp directory, clone repo, run `scripts/bootstrap.sh`
- Verify no `node_modules` or `target/` existed before script ran
- Capture output as evidence
- **Depends on:** 4.1
- **ACs covered:** clean-clone-proof, single-bootstrap

## Phase 5: CI Matrix (AC: ci-matrix)

### Step 5.1: Create GitHub Actions workflow
- Create `.github/workflows/ci.yml` with jobs:
  - `java`: setup-java-21, `./mvnw clean install`
  - `renovatio-ui`: setup-node-24, `npm ci`, `npm run build`, `npm test`
  - `renovatio-workbench`: setup-node-24, `npm ci`, `npm run build`, `npm test`
  - `python`: setup-python-3.12, `pip install`, `pytest`
  - `characterization`: needs java, run characterization tests
  - `equivalence`: needs java, run equivalence tests
- All jobs required for merge
- **Depends on:** 4.1
- **ACs covered:** ci-matrix

### Step 5.2: Verify CI runs successfully
- Push branch, trigger CI
- Verify all jobs pass
- Capture CI output as evidence
- **Depends on:** 5.1
- **ACs covered:** ci-matrix

## Phase 6: Documentation (AC: developer-parity)

### Step 6.1: Update README
- Add/update Quick Start section:
  - Prerequisites (Java 21, Node 24, Python 3.10+)
  - `git clone <repo> && cd renovatio && ./scripts/bootstrap.sh`
  - What each step does
  - How to run components individually
- **Depends on:** 4.1
- **ACs covered:** developer-parity

## Phase 7: Verification and Evidence

### Step 7.1: Run full verification
- Execute all evidence collection:
  - Clean clone build log
  - Maven build output
  - Node build outputs
  - Python test results
  - CI workflow runs
  - Bootstrap script execution
- **Depends on:** all previous steps
- **ACs covered:** all

### Step 7.2: Register artifacts and evidence
- Register `build-manifest` (toolchain versions)
- Register `ci-report` (CI workflow output)
- Register `test-report` (test results from clean build)
- Attach evidence to work item
- **Depends on:** 7.1
- **ACs covered:** all

## Dependency Graph

```
1.1 (Maven Wrapper)
 ├─→ 1.2 (JaCoCo centralize)
 ├─→ 1.3 (dependencyManagement fixes)
 │    └─→ 1.4 (hardcoded versions)
 └─→ 1.5 (Java 21 unification)
      └─→ 1.6 (Maven green verification)
           └─→ 2.4 (node-idioms collision fix)

2.1 (.nvmrc) ──┐
2.2 (UI build) ─┤
2.3 (workbench) ┤
3.1 (Python) ───┤
3.2 (migration) ┘
                │
                v
           4.1 (bootstrap.sh)
            ├─→ 4.2 (clean clone test)
            ├─→ 5.1 (CI workflow)
            │    └─→ 5.2 (CI verification)
            └─→ 6.1 (README update)
                 └─→ 7.1 (full verification)
                      └─→ 7.2 (artifacts + evidence)
```

## Risk Register

| Risk | Mitigation |
|------|-----------|
| Java 21 unification breaks compilation in shared/core/provider-java | Run full test suite after change. If tests fail, investigate specific API incompatibilities. |
| Maven Wrapper download fails in CI | Use `distributionUrl` pointing to Apache CDN, add fallback. |
| Node 24 not available in GitHub Actions | Use `actions/setup-node@v4` with explicit version. |
| Theia build requires native deps not in CI runner | Theia Dockerfile already handles this; CI may need similar deps. |
| Python provider tests depend on Java IR output | Skip Python integration tests if Java build fails, but unit tests must pass standalone. |
