# Evidence: CI Matrix

- **Type:** ci-config
- **Result:** configured
- **Date:** 2026-09-08
- **File:** .github/workflows/ci.yml

## Jobs

| Job | Runner | Dependencies | What it does |
|-----|--------|-------------|-------------|
| java | ubuntu-latest | — | setup-java-21, ./mvnw clean install |
| renovatio-ui | ubuntu-latest | java | setup-node-24, npm ci, npm test, npm run build |
| renovatio-workbench | ubuntu-latest | java | setup-node-24, npm ci, npm test, npm run build |
| python | ubuntu-latest | — | setup-python-3.12, pip install, pytest |
| characterization | ubuntu-latest | java | Characterization tests |
| equivalence | ubuntu-latest | java | Equivalence tests |

## Concurrency

- Group: ci-${{ github.ref }}
- Cancel-in-progress: true

## Caching

- Maven: ~/.m2/repository keyed on **/pom.xml
- npm (ui): renovatio-ui/node_modules keyed on package-lock.json
- npm (workbench): renovatio-workbench/node_modules keyed on package-lock.json
