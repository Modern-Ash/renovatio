# Renovatio Architecture

Renovatio follows a clean separation between the MCP protocol implementation and the core migration engine, enabling
both standalone usage and MCP client integration.

## Architecture Overview

```
┌─────────────────────┐
│   MCP Clients       │
│ (VS Code, Copilot)  │
└──────────┬──────────┘
           │ JSON-RPC 2.0 over HTTP or stdio
           │
┌──────────▼──────────┐
│  renovatio-mcp-     │
│  server             │
│  ┌─────────────────┐│
│  │ MCP Protocol    ││  McpProtocolService / McpToolingService
│  │ Implementation  ││  - tools/list, tools/call, prompts, resources
│  └─────────────────┘│  - McpToolAdapter (name mapping '.' ↔ '_')
└──────────┬──────────┘
           │ protocol-agnostic Tool API
┌──────────▼──────────┐
│  renovatio-core     │
│  (Core Engine)      │
│  ┌─────────────────┐│
│  │ Language        ││  LanguageProviderRegistry
│  │ Provider        ││  - generateTools()
│  │ Registry        ││  - route execution to providers
│  └─────────────────┘│
└──────────┬──────────┘
           │ provider SPI
    ┌──────┴──────┐
    │             │
┌───▼──┐    ┌─────▼──────┐
│ Java │    │   COBOL    │
│ Prov │    │ Provider   │
└──────┘    └────────────┘
```

- Java provider integrates OpenRewrite (recipe discovery, execution, dynamic tools)
- COBOL provider handles parsing, metrics, and migration scaffolding

## Module Structure

### renovatio-shared

- Common interfaces and domain models
- Protocol-agnostic abstractions
- Shared utilities and DTOs

### renovatio-core

- Core migration engine (protocol-agnostic)
- Language provider registry (registration, tool aggregation)
- Tool orchestration and execution
- Business logic for migration operations

### renovatio-mcp-server

- Complete MCP protocol implementation (JSON-RPC 2.0)
- Spring Boot application (HTTP mode) and stdio entrypoint
- Adapts core tools to MCP objects
- Name mapping for tools (e.g., `java.analyze` ↔ `java_analyze`)

### Language Providers

#### renovatio-provider-java

- OpenRewrite integration
- Java refactoring and migration tools
- Recipe discovery (classpath + `rewrite.yml`) and dynamic tool exposure (`java.<recipeId>`)
- Execution via JavaRecipeExecutor/OpenRewriteRunner

#### renovatio-provider-cobol

- COBOL parsing and analysis
- COBOL-to-Java migration
- Code generation capabilities
- Semantic translation pipeline powered by the COBOL IR and OpenRewrite recipes

### Supporting Modules

#### renovatio-cobol-ir

- Normalised intermediate representation for COBOL programs (data division, paragraphs, control flow)
- Lightweight parser capable of extracting statements (`MOVE`, `COMPUTE`, `IF`, `PERFORM`, embedded SQL)
- Execution context metadata used by translators

#### cobol-openrewrite-recipes

- Custom OpenRewrite recipes that consume the COBOL IR
- `PopulateCobolProcessRecipe` replaces service `process` method TODOs with Java statements derived from COBOL paragraphs
- Recipes enforce Java 17 compatibility via `HasMinimumJavaVersion`

## Runtime Modes

- HTTP mode: standard Spring Boot web application
- stdio mode: direct MCP over stdio for editor integrations

Both modes share the same core and providers. Ensure Spring scans provider packages in both entrypoints.

## Tool Lifecycle & Name Mapping

1. Provider(s) expose protocol-agnostic tools (renovatio-shared domain model)
2. Core aggregates tools via `LanguageProviderRegistry.generateTools()`
3. MCP server converts tools to MCP (`McpToolAdapter`), sanitizing names for clients:
   - Dotted names become underscored for clients that do not support dots (e.g., `java.analyze` → `java_analyze`)
   - The original name is preserved in metadata (`originalName`) so both forms are recognized
4. `tools/list` returns the merged tool list; `tools/call` routes the invocation to the appropriate provider capability

## OpenRewrite Integration (Java)

- Recipes discovered from the runtime classpath and from top-level `rewrite.yml`
- Each recipe is exposed as an MCP tool using `java.<recipeId>` (e.g., `java.org.openrewrite.java.format.AutoFormat`)
- General tools include `java.discover`, `java.analyze`, `java.plan`, `java.apply`, `java.diff`, `java.format`, `java.test`, `java.metrics`, `java.recipe_list`, `java.recipe_describe`, `java.pipeline`

## Troubleshooting

- Tool appears as "not implemented" (e.g., `java_analyze`):
  - In stdio mode, ensure the entrypoint includes provider packages in component scan:
    - `org.shark.renovatio.provider.java`
    - `org.shark.renovatio.provider.cobol`
  - Confirm project is built (`mvn clean install`) so providers are on the classpath
  - Verify `rewrite.yml` exists if relying on custom recipes and check logs for recipe discovery counts
- Tool names with dot vs underscore:
  - Use dotted names when your client supports them; the server accepts both

## Design Principles

1. Protocol Separation: core engine is independent of MCP
2. Extensibility: new language providers can be added without changing the protocol layer
3. Standards Compliance: MCP and JSON-RPC 2.0
4. Modularity: clear separation of concerns across modules
5. Simplicity: focus on core migration capabilities

## Usage Patterns

### As MCP Server

Connect MCP clients to `renovatio-mcp-server` (HTTP or stdio) for tool-based interactions.

### As Library

Use `renovatio-core` directly in applications needing migration capabilities without MCP protocol overhead.
