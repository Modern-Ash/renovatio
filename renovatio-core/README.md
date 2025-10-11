# Renovatio Core

Overview
- Core engine for Renovatio: MCP protocol support, tool catalog, and NQL routing.
- Houses core services shared by MCP server and providers.

Key Features
- Model Content Protocol (MCP) building blocks and adapters.
- NQL parsing/routing to language providers.
- Optional Spring Boot dependencies for configuration and web wiring.
- MapStruct support for DTO mapping where relevant.

Build & Test
- Build the module:
  - `mvn -DskipTests package`
- Run tests:
  - `mvn test`

Integration
- Consumed by: `renovatio-mcp-server`, `renovatio-provider-java`, and other providers.
- Exposes SPI and domain contracts via `renovatio-shared`.

Configuration
- Prefer centralized settings via `application.yml` when running in Spring contexts.

Notes
- Keep APIs MCP-compliant (input/output schemas) and stable for providers.

