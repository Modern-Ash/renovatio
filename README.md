# Renovatio - MCP Server for Code Migration and Refactoring

**Renovatio** is a Model Content Protocol (MCP) server for automated code migration and refactoring, based on
OpenRewrite concepts. It provides tools for migrating and upgrading COBOL and Java code with extensibility for
additional languages.

---

## Architecture

Renovatio is organized as a multi-module Maven project with a clean separation between the MCP protocol implementation
and the core migration engine:

```
renovatio/
├── renovatio-shared/         # Common interfaces and domain models
├── renovatio-core/           # Core migration logic (protocol-agnostic)
├── renovatio-provider-java/  # Java provider (OpenRewrite integration)
├── renovatio-provider-cobol/ # COBOL provider (parsing and migration)
└── renovatio-mcp-server/     # MCP protocol server implementation
```

---

## Module Responsibilities

### renovatio-shared

Common interfaces, domain models, and utilities shared across all modules.

### renovatio-core

Core migration logic, tool registry, and orchestration services independent of any protocol.

### renovatio-provider-java

Java language provider with OpenRewrite integration for Java refactoring and migration.

### renovatio-provider-cobol

COBOL language provider with parsing capabilities and Java code generation for COBOL-to-Java migration.

### renovatio-mcp-server

MCP protocol implementation that exposes the core migration capabilities as MCP tools following JSON-RPC 2.0
specification.

---

## Module READMEs

- [renovatio-shared](./renovatio-shared/README.md) — Shared DTOs, SPI interfaces, utilities, and NQL grammar.
- [renovatio-core](./renovatio-core/README.md) — Core MCP engine, tool catalog, and NQL routing.
- [renovatio-provider-java](./renovatio-provider-java/README.md) — Java provider with OpenRewrite integration.
- [renovatio-provider-cobol](./renovatio-provider-cobol/README.md) — COBOL provider (parsing, metrics, migration to Java).
- [renovatio-cobol-ir](./renovatio-cobol-ir/README.md) — COBOL Intermediate Representation and utilities.
- [cobol-openrewrite-recipes](./cobol-openrewrite-recipes/README.md) — OpenRewrite recipes for post-generation refactoring.
- [renovatio-mcp-server](./renovatio-mcp-server/README.md) — Spring Boot MCP server exposing providers and tools.

---

## Technology Stack

- **Java 17+**: Core platform
- **Spring Boot**: Dependency injection and configuration
- **Maven**: Build and dependency management
- **OpenRewrite**: Java refactoring engine
- **MCP (Model Content Protocol)**: Tool exposure protocol
- **JSON-RPC 2.0**: Communication protocol

---

## Quick Start

1. Build the project (root):

```bash
mvn clean install
```

2. Run the MCP server (HTTP mode):

```bash
java -jar renovatio-mcp-server/target/renovatio-mcp-server-*.jar
```

3. Run the MCP server (stdio mode, for direct MCP clients):

```bash
# Using the stdio entrypoint class
java -cp renovatio-mcp-server/target/renovatio-mcp-server-*.jar org.shark.renovatio.mcp.server.McpStdioServerApplication
```

4. Connect MCP clients (VS Code extension, Copilot Workspace, etc.) to the server to access migration tools.

---

## MCP Integration

Renovatio implements the Model Content Protocol specification, making it compatible with MCP clients like VS Code
extensions and Copilot Workspace. All tools are exposed following MCP standards with proper JSON-RPC 2.0 messaging.

### 📖 Complete MCP Client Guide

For detailed instructions on using Renovatio with MCP clients, including language-specific configurations and practical examples, see:

**[MCP Client Guide](./MCP-CLIENT-GUIDE.md)** - Comprehensive guide with:
- Language filtering (Java/COBOL)
- Client configuration examples
- Practical usage scenarios
- Best practices and troubleshooting

### Quick Start: Language Selection

Clients can request tools for a specific language by passing a `language` parameter during initialization or when listing tools:

**Initialize with language filter:**
```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "method": "initialize",
  "params": {
    "language": "java"
  }
}
```

**List tools with language filter:**
```json
{
  "jsonrpc": "2.0",
  "id": "2",
  "method": "tools/list",
  "params": {
    "language": "cobol"
  }
}
```

If `language` is omitted, all tools from all registered providers are returned.

### Configuration Examples

Pre-configured examples for different scenarios are available in the [`examples/`](./examples/) directory:
- `vscode-java-only.json` - Java-only projects
- `vscode-cobol-only.json` - COBOL migration projects
- `vscode-multi-language.json` - Full stack (Java + COBOL)
- `vscode-multiple-instances.json` - Multiple server instances

### Tool name mapping (dot ↔ underscore)

The MCP adapter sanitizes tool names for clients that do not support dots in method names. For example:
- `java.analyze` will be exposed to some clients as `java_analyze`.
- The server understands both forms and maps them internally.

When invoking tools via `tools/call`, prefer the canonical dotted name if your client supports it.

---

## Available MCP Tools

All provider tools accept a `workspacePath` parameter (added automatically by the server-side adapter when not present
in the schema) to point to the workspace directory.

### Java provider

General tools:
- `java.discover` — Inspect workspace structure
- `java.analyze` — Analyze Java sources with OpenRewrite
- `java.plan` — Plan refactoring based on goals
- `java.apply` — Apply OpenRewrite recipes
- `java.diff` — Generate git-like diff between revisions
- `java.review` — Summarize refactoring outcome
- `java.format` — Format sources and remove unused imports
- `java.test` — Run project tests
- `java.metrics` — Collect high-level Java metrics
- `java.recipe_list` — List available OpenRewrite recipes
- `java.recipe_describe` — Describe a specific recipe
- `java.pipeline` — Execute preset modernization pipeline

Dynamic recipes:
- OpenRewrite recipes discovered on the classpath and in `rewrite.yml` are exposed as tools using the pattern
  `java.<recipeId>` (e.g., `java.org.openrewrite.java.format.AutoFormat`).

### COBOL provider

- `cobol.analyze` — Analyze COBOL sources (parsing, AST, dependencies)
- `cobol.metrics` — Collect high-level COBOL metrics (files, lines, copybooks)
- `cobol.plan` — Create migration plan from COBOL to Java
- `cobol.apply` — Apply a previously created migration plan
- `cobol.diff` — Generate diff for the last migration run
- `cobol.migrate_copybook` — Generate Java artifacts from a COBOL copybook (template-based)
- `cobol.migrate_db2` — Generate JPA code from embedded DB2 EXEC SQL in COBOL programs

---

## Configuration (OpenRewrite)

- Renovatio loads OpenRewrite configuration from a top-level `rewrite.yml` if present.
- The Java provider discovers OpenRewrite recipes from the runtime classpath and from `rewrite.yml` and exposes them as
  individual MCP tools (see "Dynamic recipes").

Example `rewrite.yml` snippet:

```yaml
rewrite:
  recipes:
    - org.openrewrite.java.format.AutoFormat
    - org.openrewrite.java.cleanup.RemoveUnusedImports
```

---

## Troubleshooting

- "No real handler implemented for tool: java_analyze (internal: java.analyze)":
  - Ensure the provider packages are scanned by Spring Boot when running the MCP server, especially in stdio mode.
  - The stdio entrypoint `McpStdioServerApplication` must include provider packages in `scanBasePackages`, e.g.:

```java
@SpringBootApplication(scanBasePackages = {
    "org.shark.renovatio.mcp.server",
    "org.shark.renovatio.core",
    "org.shark.renovatio.provider.java",
    "org.shark.renovatio.provider.cobol"
})
```

- Tools not listed in `tools/list` for a given language:
  - Verify the `language` parameter passed by the client.
  - Confirm the provider module is on the classpath and built (`mvn clean install`).
  - Check `rewrite.yml` and classpath for recipe discovery.

---

## VS Code MCP client

A sample configuration file `vscode-mcp-config.json` is provided. Point it to the appropriate server mode:
- HTTP mode: the server runs as a web application.
- stdio mode: use the stdio entrypoint class for direct protocol over stdio.

---

## Contributing

- Code and documentation are in English (identifiers, comments, README, Javadoc).
- Code reviews, comments, and review suggestions from maintainers may be provided in Spanish for developer convenience.
- Follow conventional commit messages and keep modules self-contained.

---

## Lombok migration notes

This repository now uses Lombok to remove boilerplate (getters, setters, toString, and simple constructors) in core DTOs and models.

- Annotation processors: ensure they are enabled in your IDE. In IntelliJ IDEA: Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing. Install the Lombok plugin if prompted.
- JPMS (module-info.java): modules that use Lombok declare `requires static lombok;` so compilation works while keeping Lombok optional at runtime.
- Build tooling: Maven is configured to include Lombok as an annotation processor; no extra steps are needed.

If you introduce new POJOs, prefer Lombok annotations (e.g., `@Data`, or `@Getter/@Setter` plus `@NoArgsConstructor/@AllArgsConstructor`) and keep any custom setters where defensive copies are needed.

---

## 📖 Documentation Index

### Getting Started
- **[README.md](./README.md)** (this file) - Project overview and quick start
- **[MCP-QUICK-REFERENCE.md](./MCP-QUICK-REFERENCE.md)** - Quick reference for common tasks

### MCP Client Integration
- **[MCP-CLIENT-GUIDE.md](./MCP-CLIENT-GUIDE.md)** - Complete guide for MCP clients
  - Language filtering strategies
  - Practical usage examples
  - Best practices and troubleshooting
- **[examples/](./examples/)** - Pre-configured client setups
  - Java-only configuration
  - COBOL-only configuration
  - Multi-language configuration
  - Multiple server instances

### Architecture & Design
- **[ARCHITECTURE.md](./ARCHITECTURE.md)** - System architecture and design principles
- **[schemas/](./schemas/)** - JSON schemas for configuration validation

### COBOL to Python Translation (NEW) 🐍
- **[docs/COBOL-TO-PYTHON-README.md](./docs/COBOL-TO-PYTHON-README.md)** - 📚 Documentation index and quick overview
- **[docs/RESUMEN-EJECUTIVO-COBOL-PYTHON.md](./docs/RESUMEN-EJECUTIVO-COBOL-PYTHON.md)** - 🎯 Executive summary (Spanish)
- **[docs/COBOL-TO-PYTHON-IMPLEMENTATION-PLAN.md](./docs/COBOL-TO-PYTHON-IMPLEMENTATION-PLAN.md)** - 📋 Detailed implementation plan
- **[docs/COBOL-TO-PYTHON-COMPONENT-ANALYSIS.md](./docs/COBOL-TO-PYTHON-COMPONENT-ANALYSIS.md)** - 🔍 Component reusability analysis
- **[docs/COBOL-TO-PYTHON-TECHNICAL-SPEC.md](./docs/COBOL-TO-PYTHON-TECHNICAL-SPEC.md)** - 💻 Technical specification with code examples

### Planning & Specifications
- **[docs/specs/INDEX.md](./docs/specs/INDEX.md)** - 📚 Índice central de especificaciones y guías Spec Kit
- **[docs/SPEC-KIT-QUICK-START.md](./docs/SPEC-KIT-QUICK-START.md)** - ⚡ Guía rápida: Empieza en 5 minutos
- **[docs/EXPLICACION-SPEC-KIT.md](./docs/EXPLICACION-SPEC-KIT.md)** - 📖 Qué es @github/spec-kit y cómo mejora Renovatio
- **[docs/spec-kit-integracion.md](./docs/spec-kit-integracion.md)** - 🔧 Guía detallada de integración
- **[docs/specs/ejemplos/](./docs/specs/ejemplos/)** - 🎯 Especificaciones de ejemplo listas para usar

### Tool Documentation
- **Java Tools** - See section "Available MCP Tools" → "Java provider"
- **COBOL Tools** - See section "Available MCP Tools" → "COBOL provider"

---

**Renovatio** – Focused MCP server for code migration and modernization.
