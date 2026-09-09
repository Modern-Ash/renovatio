---
schema: "agora/evidence-entry/v3"
id: "evidence-000005"
work-item: "reproducible-build-ci"
swarm: "issue-223-reproducible-build-ci"
artifact: "ci.yml"
result: "pass"
date: "2026-09-09"
---

# Evidence: CI Matrix

- **Type:** ci-config
- **Result:** configured
- **Date:** 2026-09-09
- **File:** .github/workflows/ci.yml

## Jobs

| Job | Runner | Dependencies | What it does |
|-----|--------|-------------|-------------|
| java | ubuntu-latest | — | setup-java-21, setup-node-24, ./mvnw clean install -Dexec.skip=true |
| renovatio-ui | ubuntu-latest | — | setup-node-24, npm ci, npm test, npm run build |
| renovatio-workbench | ubuntu-latest | — | setup-node-24, npm ci, npm test, npm run build |
| python | ubuntu-latest | — | setup-python-3.12, pip install, pytest |
| characterization | ubuntu-latest | — | Characterization tests (offline, -Dexec.skip=true) |
| equivalence | ubuntu-latest | — | Equivalence tests (-Dexec.skip=true) |

## Concurrency

- Group: ci-${{ github.ref }}
- Cancel-in-progress: true

## Caching

- Maven: ~/.m2/repository keyed on **/pom.xml
- npm (ui): renovatio-ui/node_modules keyed on package-lock.json
- npm (workbench): renovatio-workbench/node_modules keyed on package-lock.json
