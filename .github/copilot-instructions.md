# Renovatio – Copilot & Agent Coding Instructions

Renovatio is a multi-language refactoring and migration platform, fully compatible with the Model Content Protocol (MCP) standard. It provides advanced refactoring and migration tools for Java (via OpenRewrite) and COBOL (via specialized parsers and code generation), enabling legacy application modernization and automated code refactoring. All APIs and tools are MCP-compliant and use JSON-RPC 2.0.

## Technology Stack

- **Java 17+** (core language)
- **Spring Boot** (REST API, dependency injection, configuration management)
- **Maven** (build, dependency management)
- **OpenRewrite** (Java refactoring and code analysis)
- **ProLeap/Koopa** (COBOL parsing)
- **MapStruct** (DTO mapping)
- **Lombok** (automatic generation of DTOs and entity boilerplate)
- **Freemarker** (template-based code generation)
- **Apache Lucene** (search and indexing)
- **DB2, JPA/Hibernate** (database migration and ORM)
- **Zowe/JCICS** (CICS integration)
- **Shell, GitHub Actions, Spring Batch, Airflow** (JCL conversion and automation)
- **JUnit 5 & RestAssured** (testing, including integration and REST endpoint tests)
- **OpenAPI/Swagger** (API documentation)

## Architecture & Design

- **Modular structure:**
  - `renovatio-core`: Core logic and shared services
  - `renovatio-provider-cobol`: COBOL language provider and migration tools
  - `renovatio-provider-java`: Java language provider and refactoring tools
  - `renovatio-shared`: Shared models, DTOs, and utilities
  - `renovatio-web`: Web and API layer
- **Layered design:**
  - Controller (REST/MCP endpoints)
  - Service (business logic)
  - Repository (data access)
  - Model/Entity (domain models)
- **Extensible:** New languages and tools can be added as MCP modules.
- **Configuration:** Managed via `application.yml` for all modules.
- **MCP Compliance:** All endpoints and tools follow MCP schemas for input/output.
- **Interoperability:** Designed for integration with MCP clients (VS Code, Copilot Workspace, etc.).

## Coding Guidelines

- Use English for all comments, documentation, and identifier names.
- Follow Java and Spring Boot best practices (naming, dependency injection, exception handling).
- Write modular, clean, and well-documented code.
- Use meaningful commit messages (see `git-commit-instructions.md`).
- Maintain and write tests for all new features (JUnit 5, RestAssured for REST endpoints).
- Ensure all code and endpoints are MCP-compliant for input/output schemas.
- Prefer configuration via `application.yml`.
- Document any new tool, endpoint, or module in the README and relevant documentation.

## Lombok usage
- Lombok is **mandatory** for all Java model, entity, and DTO classes. Always use Lombok annotations (e.g., `@Data`, `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`) to reduce boilerplate. Remove manual getters/setters, constructors, and `equals/hashCode/toString` if Lombok can generate them.
- Ensure Lombok is present as a dependency in all Maven modules (`pom.xml`).
- If you add a new Java class that could use Lombok, annotate it accordingly and do not write boilerplate code manually.

## Business Context

- Renovatio provides unified APIs for refactoring, migration, and code generation to modernize legacy applications.
- Supports Java and COBOL, with extensibility for additional languages and tools.
- Enables automated migration, refactoring, and code analysis for enterprise applications.
- Designed for seamless interoperability with MCP clients and agent-based workflows.

## When using GitHub Copilot or other agents:

- Follow the existing code style and conventions.
- Use English for all comments and documentation.
- Ensure code is clean, modular, and well-documented.
- Write meaningful commit messages (see `git-commit-instructions.md`).
- Prefer configuration via `application.yml`.
- If you add new features, update the documentation (README, docs).
- Ensure all new code and endpoints are MCP-compliant.

---

For more details on agent interoperability and best practices, see [agents.md](https://agents.md/).


# Feature notes: 1-cobol-python-migration

This repository contains a feature under `specs/1-cobol-python-migration` that implements a proof-of-concept pipeline to migrate COBOL programs to Python.

Key artifacts for agents and copilot:
- Spec: `specs/1-cobol-python-migration/spec.md`
- Research: `specs/1-cobol-python-migration/research.md`
- Data model: `specs/1-cobol-python-migration/data-model.md`
- Templates & generator: `specs/1-cobol-python-migration/templates/` and `tools/generate.py`
- Tests and examples: `specs/1-cobol-python-migration/examples/`, `tests/`
- Tasks & plan: `specs/1-cobol-python-migration/tasks.md`, `specs/1-cobol-python-migration/plan.md`

Agents should preserve manual notes and not overwrite these files unless explicitly instructed in a PR. When updating agent-context for this feature, prefer adding details to `specs/1-cobol-python-migration/agent-context-copilot.md`.

## Active Technologies
- Java 17+, Spring Boot 3.x + Spring Data JPA, Spring Web, Spring Test, JUnit 5, Mockito, RestAssured, OpenRewrite API, ProLeap/Koopa COBOL parser (001-cobol-python-migration)
- PostgreSQL/DB2 via JPA/Hibernate (migrations with Flyway) (001-cobol-python-migration)

## Recent Changes
- 001-cobol-python-migration: Added Java 17+, Spring Boot 3.x + Spring Data JPA, Spring Web, Spring Test, JUnit 5, Mockito, RestAssured, OpenRewrite API, ProLeap/Koopa COBOL parser
