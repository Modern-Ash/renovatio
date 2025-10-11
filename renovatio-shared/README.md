# Renovatio Shared

Overview
- Shared domain models, SPI interfaces, utilities, and the NQL grammar used across Renovatio.
- Distributed as an open Java module exporting `domain`, `nql`, `spi`, and `util` packages.

Key Features
- Provider SPI (base contracts for language providers and tools).
- Domain DTOs (AnalyzeResult, PlanResult, Workspace, etc.).
- NQL grammar and generated sources via ANTLR.
- Utilities common to core and providers.

Build & Test
- Build the module:
  - `mvn -DskipTests package`
- Run tests:
  - `mvn test`

Integration
- Consumed by most Renovatio modules (core, providers, server).
- Lombok used for DTOs; ensure annotation processing is enabled in your IDE/build.

Notes
- Keep changes backward compatible to avoid breaking providers.
- Update the module descriptor (`module-info.java`) if you add new exported packages.
