````markdown
# Jira Stories Mapping for Renovatio Spec-Driven Development

**Updated**: 2025-11-26  
**Project**: Renovatio - Multi-language refactoring and migration platform  
**Version**: 1.0.0  
**Purpose**: Links Renovatio Jira issues with spec-driven development framework

## Renovatio Overview

**Technology Stack**: Java 17+, Spring Boot, Maven, OpenRewrite, ProLeap/Koopa  
**Architecture**: Modular monorepo (renovatio-core, renovatio-provider-*, renovatio-shared, renovatio-web, renovatio-mcp-server)  
**MCP Compliance**: All tools exposed as JSON-RPC 2.0 compliant endpoints  

This document shows how Renovatio features map to Jira stories, SDD templates, and constitutional principles.

## Example Epic: Java Refactoring Recipe Framework

### Epic Overview

**Epic Title**: "Implement Custom Java Refactoring Recipe Framework"  
**Jira Key**: REN-100 (example)  
**Description**: Enable developers to create, test, and deploy custom Java refactoring recipes alongside OpenRewrite recipes, with full MCP compliance and integrated testing support.

**Constitutional Alignment**:
- ✅ **Module & Package First**: New module `renovatio-provider-java-custom-recipes` with independent test suite
- ✅ **Test-First**: Unit tests for recipe validation, contract tests for recipe API contracts, integration tests with real code samples
- ✅ **API/Contract-First**: Define RecipeDefinition OpenAPI schema and MCP tool schema for recipe invocation
- ✅ **Observability**: Structured logging of recipe execution, metrics on recipe success rates
- ✅ **Versioning**: Semantic versioning for recipe registry, breaking changes tracked in migration guides

## User Stories & Spec-Driven Development Mapping

### User Story 1 (P1) - Recipe Definition & Validation

**Jira Key**: REN-101  
**Description**: Allow developers to define Java refactoring recipes using a declarative schema with validation and schema documentation.

**Constitutional Alignment**:
- ✅ **Module & Package First**: Recipe definition logic in `renovatio-provider-java-custom-recipes` module
- ✅ **Test-First**: Unit tests for recipe parser, contract tests for recipe schema
- ✅ **API/Contract-First**: RecipeDefinition JSON schema must be defined before parser implementation
- ✅ **Observability**: Log recipe parsing events with correlation IDs
- ✅ **Versioning**: Recipe schema changes require MAJOR version if breaking existing recipes

**Subtasks**:
- REN-101.1: Design RecipeDefinition JSON schema (specify recipe structure, supported transformations, rule syntax)
- REN-101.2: Implement RecipeParser (Java class to parse and validate RecipeDefinition from JSON/YAML)
- REN-101.3: Create unit tests for RecipeParser (JUnit 5, `*Test.java`)
- REN-101.4: Create contract tests validating RecipeDefinition schema
- REN-101.5: Implement error codes and custom exceptions for recipe validation (RecipeParseException, etc.)
- REN-101.6: Add structured logging to recipe validation with MDC correlation IDs

### User Story 2 (P2) - Recipe Testing Framework

**Jira Key**: REN-102  
**Description**: Provide testing utilities for developers to verify recipes work as expected on sample code before deployment.

**Constitutional Alignment**:
- ✅ **Module & Package First**: Recipe testing logic in `renovatio-provider-java-custom-recipes` (dedicated test package)
- ✅ **Test-First**: Unit tests for test framework, integration tests with sample Java code
- ✅ **API/Contract-First**: RecipeTest OpenAPI schema defining test input/output format
- ✅ **Observability**: Log test execution results and timing metrics
- ✅ **Versioning**: Breaking changes to test output format require MAJOR version bump

**Subtasks**:
- REN-102.1: Design RecipeTest schema (code sample, expected result, assertion rules)
- REN-102.2: Implement RecipeTestRunner (execute recipe on sample code, compare results)
- REN-102.3: Create unit tests for RecipeTestRunner
- REN-102.4: Create integration tests with real Java code samples
- REN-102.5: Add test metrics (execution time, assertion pass/fail rates) to Spring Boot Actuator
- REN-102.6: Implement detailed error reporting for failed assertions

### User Story 3 (P3) - REST API for Recipe Management

**Jira Key**: REN-103  
**Description**: Expose recipe management via REST API (create, read, update, delete recipes) with full OpenAPI documentation.

**Constitutional Alignment**:
- ✅ **Module & Package First**: RecipeController in `renovatio-web` module, delegates to `renovatio-provider-java-custom-recipes`
- ✅ **Test-First**: Unit tests for RecipeController, integration tests via RestAssured
- ✅ **API/Contract-First**: OpenAPI/Swagger specification must define all recipe endpoints before implementation
- ✅ **Observability**: All API calls logged with correlation IDs, audit trail for recipe changes
- ✅ **Versioning**: API endpoint versioning (e.g., `/api/v1/recipes`) required for major changes

**Subtasks**:
- REN-103.1: Design OpenAPI specification for Recipe REST endpoints (`POST /api/v1/recipes`, `GET /api/v1/recipes/{id}`, etc.)
- REN-103.2: Implement RecipeController with CRUD endpoints
- REN-103.3: Create unit tests for RecipeController (mock dependencies)
- REN-103.4: Create integration tests with RestAssured and TestContainers
- REN-103.5: Add audit logging (who, when, what changed) to recipe operations
- REN-103.6: Implement OpenAPI documentation generation and validation

### User Story 4 (P3) - MCP Tool Integration

**Jira Key**: REN-104  
**Description**: Expose recipe execution as MCP-compliant tool, allowing integration with MCP clients (VS Code, agents, etc.).

**Constitutional Alignment**:
- ✅ **Module & Package First**: RecipeTool wrapper in `renovatio-mcp-server` module
- ✅ **Test-First**: Unit tests for tool schema compliance, integration tests with MCP protocol
- ✅ **API/Contract-First**: JSON-RPC 2.0 tool schema (input params, output format, error codes)
- ✅ **Observability**: Log tool invocations with correlation IDs, metrics on invocation success/failure
- ✅ **Versioning**: Tool schema changes require MAJOR version bump (breaking client compatibility)

**Subtasks**:
- REN-104.1: Design JSON-RPC 2.0 tool schema for recipe execution (input: recipe ID + code, output: refactored code)
- REN-104.2: Implement RecipeTool (JSON-RPC wrapper for recipe execution)
- REN-104.3: Create unit tests for tool schema compliance
- REN-104.4: Create integration tests invoking tool via MCP protocol
- REN-104.5: Add error mapping (RecipeParseException → INVALID_PARAMS, etc.)
- REN-104.6: Add tool introspection/documentation (schema, examples)

## Spec-Driven Development Implementation Plan

### Phase 1: Specification & Planning

**Deliverable**: Spec document (spec.md) with all user stories, requirements, and acceptance criteria

1. **Create feature specification** following `.specify/templates/spec-template.md`
   - User stories (P1-P3) with acceptance scenarios
   - Functional requirements with observability expectations
   - Success criteria with code quality gates

2. **Research and technical decisions** (research.md)
   - Evaluate recipe definition formats (JSON vs YAML vs custom DSL)
   - Decide on parser implementation (hand-written vs grammar-based)
   - Identify OpenRewrite recipe compatibility needs

3. **Data model design** (data-model.md)
   - RecipeDefinition entity (JPA @Entity, persisted in DB)
   - RecipeTest entity for test cases
   - RecipeExecution entity for audit trail
   - Database schema with Flyway migrations

4. **API contracts** (contracts/ directory)
   - RecipeDefinition OpenAPI schema (POST /api/v1/recipes)
   - RecipeTool JSON-RPC schema for MCP compliance
   - Error response schemas with error codes

5. **Quickstart guide** (quickstart.md)
   - How to build: `mvn clean package`
   - How to run tests: `mvn test`, `mvn verify`
   - How to test locally: IDE run configuration + curl examples

### Phase 2: Foundational Infrastructure

**Scope**: Database, configuration, observability setup (BLOCKING all user stories)

- [ ] INFRA-1: Create `renovatio-provider-java-custom-recipes` module structure
- [ ] INFRA-2: Add database schema for recipes (Flyway migration)
- [ ] INFRA-3: Configure Spring Boot actuator for metrics and health checks
- [ ] INFRA-4: Setup structured logging with correlation IDs (MDC configuration)
- [ ] INFRA-5: Define error code enums (RecipeParseError, TestExecutionError, etc.)

**Checkpoint**: Foundation ready - user story work can now begin in parallel

### Phase 3: User Story Implementation (In Priority Order)

**US1 - Recipe Definition & Validation (REN-101)** [P1] → MVP
- [ ] T101-1: Contract test for RecipeDefinition schema (must fail first)
- [ ] T101-2: Integration test for RecipeParser (must fail first)
- [ ] T101-3: Create RecipeDefinition model (JPA @Entity)
- [ ] T101-4: Implement RecipeParser with validation
- [ ] T101-5: Document error codes in module README
- [ ] T101-6: Add observability logging
- **Deliverable**: Can define and validate Java recipes

**US2 - Recipe Testing Framework (REN-102)** [P2]
- [ ] T102-1: Contract test for RecipeTest schema (must fail first)
- [ ] T102-2: Integration test for RecipeTestRunner on sample Java code (must fail first)
- [ ] T102-3: Create RecipeTest model
- [ ] T102-4: Implement RecipeTestRunner
- [ ] T102-5: Add metrics to Spring Boot Actuator
- [ ] T102-6: Create sample recipes for testing
- **Deliverable**: Can test recipes on code samples before deployment

**US3 - REST API for Recipe Management (REN-103)** [P3]
- [ ] T103-1: Contract test for OpenAPI endpoints (must fail first)
- [ ] T103-2: Integration test for RecipeController via RestAssured (must fail first)
- [ ] T103-3: Implement RecipeController with CRUD endpoints
- [ ] T103-4: Add audit logging to recipe changes
- [ ] T103-5: Generate and validate OpenAPI documentation
- [ ] T103-6: Create REST client documentation with curl examples
- **Deliverable**: REST API for recipe management

**US4 - MCP Tool Integration (REN-104)** [P3]
- [ ] T104-1: Contract test for MCP tool schema (must fail first)
- [ ] T104-2: Integration test for MCP protocol compliance (must fail first)
- [ ] T104-3: Implement RecipeTool MCP wrapper
- [ ] T104-4: Add tool introspection endpoint
- [ ] T104-5: Document tool usage in MCP client guide
- **Deliverable**: Recipes available to MCP clients

## Constitution Checklist per User Story

### REN-101 (Recipe Definition & Validation)

- [ ] **Tests-First**: Unit tests for parser written first, failing before implementation
- [ ] **Contract Tests**: RecipeDefinition schema defined in `src/main/resources/schemas/`, tested before parser
- [ ] **Integration Tests**: Real recipe files tested with RecipeParser
- [ ] **Observability**: Structured logging with correlation IDs, error codes defined
- [ ] **Versioning**: RecipeDefinition schema v1.0.0 in pom.xml, breaking changes documented

### REN-102 (Recipe Testing)

- [ ] **Tests-First**: RecipeTestRunner tests written first, failing
- [ ] **Contract Tests**: RecipeTest schema defined and tested
- [ ] **Integration Tests**: Run recipes on sample Java code, verify results
- [ ] **Observability**: Metrics for test execution time, success/failure rates
- [ ] **Versioning**: RecipeTest schema v1.0.0, migration guide for changes

### REN-103 (REST API)

- [ ] **Tests-First**: RecipeController unit tests + RestAssured integration tests, failing first
- [ ] **Contract Tests**: OpenAPI specification defined in `src/main/resources/openapi.yaml`, validated
- [ ] **Integration Tests**: TestContainers with real database, full CRUD workflow tested
- [ ] **Observability**: Audit logs for all recipe mutations, metrics on endpoint latency
- [ ] **Versioning**: API v1.0.0, endpoint versioning strategy documented

### REN-104 (MCP Tool)

- [ ] **Tests-First**: MCP tool schema tests + protocol compliance tests, failing first
- [ ] **Contract Tests**: JSON-RPC 2.0 schema defined, validated against protocol spec
- [ ] **Integration Tests**: Tool invoked via MCP client, results verified
- [ ] **Observability**: Tool invocation logged with tool name, args, results, timing
- [ ] **Versioning**: Tool schema v1.0.0, breaking schema changes tracked in changelog

## Implementation Priority (Recommended Sequencing)

1. **Phase 0: Specification** (1-2 days)
   - Write spec.md with user stories and requirements
   - Create research.md with technical decisions
   
2. **Phase 1: Design** (2-3 days)
   - Define data model (RecipeDefinition, RecipeTest entities)
   - Write API contracts (OpenAPI + MCP schemas)
   - Create database migrations
   
3. **Phase 2: Foundational Infrastructure** (1-2 days)
   - Create Maven module structure
   - Setup database, logging, error codes
   - Configure Spring Boot actuator
   
4. **Phase 3: User Stories (Sequential or Parallel)**
   - **REN-101** (2-3 days): Recipe parsing - **CRITICAL PATH** (blocks all others)
   - **REN-102** (2-3 days): Recipe testing - Depends on REN-101
   - **REN-103** (2-3 days): REST API - Independent, can start after REN-101
   - **REN-104** (1-2 days): MCP tool - Can start after REN-101

5. **Phase 4: Polish & Validation** (1 day)
   - Verify all tests pass: `mvn clean verify`
   - Validate OpenAPI documentation
   - Run performance and security scans

## Maven Build & CI/CD Integration

### Local Build

```bash
cd renovatio-provider-java-custom-recipes
mvn clean package                 # Full build with unit tests
mvn test                          # Run unit tests only
mvn verify                        # Run unit + integration tests
mvn -DskipTests package           # Build without tests
```

### CI Gates (GitHub Actions)

```yaml
# Must pass before PR merge:
- mvn clean package               # Build success
- mvn test                        # Unit tests (JUnit 5)
- mvn verify                      # Integration tests (RestAssured)
- mvn dependency-check:check      # CVE scanning
- mvn checkstyle:check            # Code style validation
```

## Next Steps for Implementation

1. **Create feature specification**: Use `.specify/templates/spec-template.md`
2. **Generate research document**: Use `.specify/templates/plan-template.md` Phase 0
3. **Design data model & contracts**: Use `.specify/templates/plan-template.md` Phase 1
4. **Create task breakdown**: Use `.specify/templates/tasks-template.md`
5. **Begin implementation**: Start with INFRA phase, then REN-101 (P1)

## References

- [Renovatio Constitution](.specify/memory/constitution.md): SDD principles for this project
- [Spec-Driven Development Guide](docs/SPEC_DRIVEN_DEVELOPMENT.md): How to use SDD in Renovatio
- [Renovatio Architecture](ARCHITECTURE.md): Modular structure and module responsibilities
- [MCP Quick Reference](MCP-QUICK-REFERENCE.md): MCP schema and tool definitions
- [OpenRewrite Documentation](https://docs.openrewrite.org/)

## Jira Integration Points

Each spec document should reference:
- **Epic**: REN-100 (example) in feature title
- **User Stories**: REN-101, REN-102, etc. as priorities P1, P2, P3
- **Subtasks**: Individual REN-X.Y issues as implementation tasks
- **Links**: Direct links to Jira issues in task descriptions

**Jira URL Pattern**: `https://faguero.atlassian.net/browse/REN-X`

This creates full traceability from spec-driven development artifacts back to original Jira planning.

````
