````markdown
# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]  
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`  
**Stack**: Java 17+, Spring Boot, Maven, MCP-compliant

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the Renovatio project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Java 17+, Spring Boot 3.x  
**Primary Dependencies**: Spring Data JPA, Spring Web, Spring Test, JUnit 5, Mockito, RestAssured, OpenRewrite API, ProLeap/Koopa COBOL parser  
**Storage**: PostgreSQL/DB2 via JPA/Hibernate (migrations with Flyway)  
**Testing**: JUnit 5 (unit tests), RestAssured (integration tests), TestContainers (database testing), contract tests with OpenAPI/MCP schemas  
**Target Platform**: Docker containers, Kubernetes/Cloud-native deployment  
**Project Type**: Maven modular monorepo (renovatio-core, renovatio-provider-*, renovatio-shared, renovatio-web, plus optional new modules)  
**API Exposure**: REST endpoints (Spring MVC) + MCP-compliant tools (JSON-RPC 2.0)  
**Performance Goals**: [e.g., "Parse 10K lines of code in <2 seconds", "Handle 100 concurrent refactoring operations"]  
**Constraints**: [e.g., "<1GB memory footprint", "No external service calls during migrations", "Full backward compatibility with existing MCP clients"]  
**Scale/Scope**: [e.g., "Support Java, COBOL, and future language providers", "Handle 1000+ refactoring plans/day"]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The following gates are derived from Renovatio's Spec-Driven Development Constitution v1.0.0
and MUST be verified for [FEATURE] implementation:

- **Module & Package First**: Is this feature best implemented as a reusable Maven module or extension? 
  - If YES: Create dedicated `renovatio-provider-*` or `renovatio-*` module with clear interfaces
  - If NO: Explain why it belongs in existing module (renovatio-core, renovatio-shared, renovatio-web)
  - All code MUST have clear API contracts with Javadoc

- **Test-First (NON-NEGOTIABLE)**: 
  - Unit tests MUST be written before implementation (JUnit 5, located in `src/test/java/*Test.java`)
  - Integration tests MUST verify cross-module behavior (RestAssured, `src/test/java/*IntegrationTest.java`)
  - Contract tests MUST validate OpenAPI/MCP schemas
  - All tests MUST fail initially before implementation

- **API/Contract-First**: 
  - All REST endpoints MUST have OpenAPI/Swagger specifications
  - All MCP tools MUST define JSON-RPC 2.0 input/output schemas
  - Schemas MUST be tested before endpoint implementation
  - Example: `src/main/resources/schemas/MyToolRequest.json` + `src/main/resources/schemas/MyToolResponse.json`

- **Observability & Error Handling**: 
  - All components MUST use `@Slf4j` (Lombok) for structured logging
  - Correlation IDs MUST be propagated via MDC (Mapped Diagnostic Context)
  - All error paths MUST map to documented error codes (see `ErrorCode` enum)
  - Metrics MUST be emitted via Spring Boot Actuator for key operations
  - Example: `MDC.put("correlationId", uuid); log.info("Processing refactoring plan", "planId", planId);`

- **Versioning & Backward Compatibility**: 
  - New modules MUST declare semantic versioning in `pom.xml` (`<version>1.0.0</version>`)
  - Breaking API/MCP schema changes MUST increment MAJOR version
  - Migration plans MUST be documented for breaking changes
  - Example: "Changing tool input schema from X to Y requires MAJOR version bump and client documentation"

**Security Specific Gates**:
- No secrets (API keys, passwords, credentials) in code or pom.xml - use environment variables or Spring properties
- All external dependencies MUST pass CVE scanning (OWASP Dependency-Check)
- Database migrations MUST protect sensitive data (e.g., no logging of credentials)
- MCP tool inputs MUST sanitize any user-provided data

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command) - OpenAPI + MCP schemas
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (Renovatio Modular Monorepo)

```text
# Example: New Language Provider or Refactoring Tool Feature

renovatio-provider-[language]/          # NEW: Language-specific provider
├── pom.xml                             # Define version, dependencies, plugins (Surefire, Failsafe, JAR packaging)
├── src/
│   ├── main/
│   │   ├── java/org/renovatio/provider/[language]/
│   │   │   ├── LanguageProviderImpl.java      # Implement LanguageProvider interface
│   │   │   ├── parser/                       # Parsing logic
│   │   │   ├── analyzer/                     # Analysis logic
│   │   │   ├── model/                        # Domain models (JPA @Entity if persisting)
│   │   │   ├── config/                       # Configuration classes
│   │   │   └── error/                        # Error codes + custom exceptions
│   │   └── resources/
│   │       ├── application.yml               # Provider-specific config
│   │       ├── schemas/                      # OpenAPI/MCP tool schemas (JSON)
│   │       └── db/migration/                 # Flyway migrations (if DB changes)
│   └── test/
│       └── java/org/renovatio/provider/[language]/
│           ├── LanguageProviderImplTest.java     # Unit tests
│           ├── parser/*Test.java
│           ├── analyzer/*Test.java
│           └── integration/
│               └── *IntegrationTest.java         # Integration tests with Spring context

renovatio-shared/                       # EXTENDED: Add DTOs/models for new tool
├── src/
│   ├── main/java/org/renovatio/shared/
│   │   ├── dto/                         # DTOs for new tool inputs/outputs
│   │   ├── model/                       # Shared domain models
│   │   └── api/                         # Shared interfaces/contracts
│   └── test/java/org/renovatio/shared/
│       └── *Test.java

renovatio-web/                          # EXTENDED: Add REST endpoints for new tool/provider
├── src/
│   ├── main/java/org/renovatio/web/
│   │   ├── controller/
│   │   │   └── [Feature]Controller.java       # New REST endpoints
│   │   ├── service/                           # Business logic (delegate to provider)
│   │   └── config/
│   │       └── MvcConfig.java                 # Enable OpenAPI generation
│   └── test/java/org/renovatio/web/
│       ├── controller/*Test.java              # Unit tests
│       └── integration/*IntegrationTest.java  # Integration tests

renovatio-mcp-server/                   # EXTENDED: Add MCP tool registration
├── src/
│   ├── main/java/org/renovatio/mcp/
│   │   ├── tool/
│   │   │   └── [Feature]Tool.java             # MCP tool wrapper
│   │   ├── schema/                            # Schema definitions
│   │   └── handler/                           # Tool invocation handlers
│   └── test/java/org/renovatio/mcp/
│       └── tool/*Test.java

# Directory structure for implementation
db/migration/                           # Database migrations (new tables, etc.)
├── V1_0__initial_schema.sql
├── V1_1__add_[feature]_tables.sql
└── V1_2__add_audit_logging.sql
```

**Structure Decision**: Extends existing Renovatio modular architecture. New language providers go into `renovatio-provider-*` modules. 
Shared code goes into `renovatio-shared`. REST endpoints into `renovatio-web`. MCP tool definitions into `renovatio-mcp-server`. 
This maintains separation of concerns, independent testing, and clear module boundaries.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [If applicable: new external dependency] | [Justification] | [Why simpler approach doesn't work] |
| [If applicable: breaking API change] | [Justification] | [Alternative compatibility approach considered] |

## Key Deliverables

1. **Spec Document**: Feature specification with user stories and acceptance criteria
2. **Research Document**: Technical decisions, alternatives evaluated, dependencies identified
3. **Data Model**: Domain entities, JPA mappings, database schema (with migrations)
4. **API Contracts**: OpenAPI/Swagger for REST endpoints, JSON-RPC schemas for MCP tools
5. **Task Breakdown**: Detailed implementation tasks by user story, with test-first approach
6. **Quickstart Guide**: How to build, test, and run the feature locally

## Next Steps

1. Complete Phase 0: Research (resolve all NEEDS CLARIFICATION items)
2. Complete Phase 1: Design (data model, contracts, agent context update)
3. Generate Phase 2: Tasks breakdown (ready for implementation)

````
