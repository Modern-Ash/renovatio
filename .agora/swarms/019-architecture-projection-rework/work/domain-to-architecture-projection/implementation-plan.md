# Implementation plan

1. Define `ArchitectureModel v1` with components, modules, ports, adapters, relations, diagnostics, requested/effective style, and evidence references.
2. Implement projections for Transaction Script and Hexagonal from DomainModel; add a capability declaration for Layered MVC and activate only with a real profile/layout.
3. Add adapters to existing architecture preview/manifest and TargetModel consumers without direct COBOL dependencies.
4. Add style/fallback/determinism/compatibility tests, then record verification and review artifacts.
