# Renovatio Java Provider

Overview
- Java language provider for Renovatio with OpenRewrite-based refactoring and MCP tool exposure.
- Discovers and executes recipes defined in `rewrite.yml`.

Key Features
- Integration with OpenRewrite (java, yaml, java-17 parsers).
- Git-aware refactoring when needed (JGit).
- Spring-based configuration for easy wiring.

Build & Test
- Build the module:
  - `mvn -DskipTests package`
- Run tests:
  - `mvn test`

Usage
- Provide a `rewrite.yml` in the module or workspace to define recipes.
- The provider can be used standalone or through the MCP server.

Dependencies
- Depends on: `renovatio-core`, `renovatio-shared`.
- Uses OpenRewrite, Spring Boot (core starter), and Lombok.

Notes
- Ensure recipes remain idempotent and safe to apply incrementally.

