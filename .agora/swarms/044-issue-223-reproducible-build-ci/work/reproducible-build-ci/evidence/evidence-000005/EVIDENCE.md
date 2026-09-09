---
schema: "agora/evidence-entry/v3"
id: "evidence-000005"
work-item: "reproducible-build-ci"
swarm: "issue-223-reproducible-build-ci"
artifact: "ci.yml"
result: "pass"
date: "2026-09-09"
content-hash: "sha256:3d0e1818f23e0e71724989ad239adc1ff868f7fe7ce10e735f3e6712634231c5"
source-ref: "0ad4fd45b37a4566c8b164f6179aae51c9cd20c9"
---

# Evidence: CI Matrix

- **Type:** ci-config
- **Result:** configured
- **Date:** 2026-09-09
- **File:** .github/workflows/ci.yml
- **Content Hash:** sha256:3d0e1818f23e0e71724989ad239adc1ff868f7fe7ce10e735f3e6712634231c5

## Jobs

| Job | Runner | Dependencies | What it does |
|-----|--------|-------------|-------------|
| java | ubuntu-latest | — | setup-java-21, setup-node-24, ./mvnw clean install -Dexec.skip=true |
| renovatio-ui | ubuntu-latest | java | setup-node-24, npm ci, npm test, npm run build |
| renovatio-workbench | ubuntu-latest | java | setup-node-24, npm ci, npm test, npm run build |
| python | ubuntu-latest | — | setup-python-3.12, pip install, pytest |
| characterization | ubuntu-latest | java | Characterization tests (offline, -Dexec.skip=true) |
| equivalence | ubuntu-latest | java | Equivalence tests (-Dexec.skip=true) |
| bootstrap | ubuntu-latest | — | Full bootstrap.sh integration test |
| pip-audit | ubuntu-latest | — | Python security audit (blocking) |

## Concurrency

- Group: ci-${{ github.ref }}
- Cancel-in-progress: true

## Caching

- Maven: ~/.m2/repository keyed on **/pom.xml
- npm (ui): renovatio-ui/node_modules keyed on package-lock.json
- npm (workbench): renovatio-workbench/node_modules keyed on package-lock.json

## Verification

```bash
# Verify CI config hash
sha256sum .github/workflows/ci.yml
# Expected: 3d0e1818f23e0e71724989ad239adc1ff868f7fe7ce10e735f3e6712634231c5
```
