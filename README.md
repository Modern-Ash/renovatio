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

### Language selection (Java/COBOL) from MCP clients

Clients can request tools for a specific language by passing a `language` parameter. This helps surface only the
relevant tools for the chosen language:

- During `initialize`:

```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "method": "initialize",
  "params": {
    "language": "cobol"
  }
}
```

- When listing tools:

```json
{
  "jsonrpc": "2.0",
  "id": "2",
  "method": "tools/list",
  "params": {
    "language": "java"
  }
}
```

If `language` is omitted, all tools from all registered providers are returned.

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

**Renovatio** – Focused MCP server for code migration and modernization.
