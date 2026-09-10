# Renovatio Spec-Driven Development Constitution

<!--
Adapted for Renovatio – A multi-language refactoring and migration platform.

Version: Renovatio 1.0.0 (adapted from TravelCBooster v1.0.0)

Core principles retained from TravelCBooster:
- [PRINCIPLE_1] "Module & Package First" (Maven modules/packages)
- [PRINCIPLE_2] "Test-First (NON-NEGOTIABLE)" (JUnit 5, RestAssured, contract tests)
- [PRINCIPLE_3] "API / Contract-First" (OpenAPI/Swagger, MCP schemas)
- [PRINCIPLE_4] "Observability & Error Handling" (Structured logging, Spring Boot metrics)
- [PRINCIPLE_5] "Versioning & Backward Compatibility" (Semantic versioning for modules)

Technology context (Renovatio stack):
- Language: Java 17+ with Spring Boot
- Build: Maven
- Testing: JUnit 5, RestAssured
- API: OpenAPI/Swagger, MCP compliance (JSON-RPC 2.0)
- Architecture: Modular (renovatio-core, renovatio-provider-*, renovatio-shared, renovatio-web)
- Database: JPA/Hibernate, DB2/PostgreSQL
- Code Generation: Freemarker, Lombok, MapStruct

Adapted sections:
- "Library & Package First" → "Module & Package First" (Maven modules)
- Test examples: Python pytest → JUnit 5 / RestAssured
- Observability: Generic JSON logs → Spring Boot structured logging
- API contracts: OpenAPI (FastAPI) → OpenAPI/Swagger (Spring Boot) + MCP schemas

Templates updated for Java/Renovatio:
- .specify/templates/spec-template.md ✅ updated
- .specify/templates/plan-template.md ✅ updated
- .specify/templates/tasks-template.md ✅ updated
- .specify/templates/checklist-template.md ✅ updated

Follow-up TODOs:
- Review MCP compliance gates in specific features
- Update agent context scripts for Maven/Java paths
-->

## Core Principles

### Module & Package First
Every new feature MUST prefer a reusable Maven module or package when it produces shared
behavior, data models, or language providers. Code intended to be consumed by multiple
applications MUST live in a dedicated Maven module under `renovatio-*/` or a clearly
named package. All new modules MUST be self-contained, include clear documentation,
a comprehensive test suite, and follow MCP schemas for tool definitions. Examples include:
new language provider (renovatio-provider-xyz), shared utilities (renovatio-shared extensions),
or core refactoring tools. Rationale: avoids duplication, simplifies releases, enables
independent versioning, and maintains MCP compliance across the platform.

### Test-First (NON-NEGOTIABLE)
All work MUST follow a test-first workflow. For each change, tests are written before
production code, and tests MUST fail initially. Required test types depend on scope but
MUST include: unit tests (JUnit 5) for logic, contract tests for public interfaces
(OpenAPI/MCP schemas), and integration tests (RestAssured) for cross-module behavior.
All tests MUST be located in `src/test/java` with naming pattern `*Test.java` and
execute via Maven `test` lifecycle. Rationale: prevents regressions, ensures deliverables
are independently verifiable, and maintains code quality standards across modules.

### API / Contract-First
Public service interfaces and shared schemas MUST be defined as explicit contracts.
For REST endpoints: OpenAPI/Swagger specifications (via SpringDoc-OpenAPI). For MCP tools:
JSON-RPC 2.0 schemas with input/output specifications. For shared classes: Clear interfaces
with Javadoc. Any change to a contract MUST be accompanied by contract tests and a
compatibility statement (see Versioning). Rationale: clear expectations between teams,
automated verification of behavioral guarantees, and seamless MCP client interoperability.

### Observability & Error Handling
Every service and library MUST emit structured logs (JSON via Spring Boot Logback configuration),
include meaningful metrics (Spring Boot Actuator), and map errors to documented error codes.
All Spring Boot endpoints MUST include correlation IDs (MDC - Mapped Diagnostic Context) for
tracing requests across modules. MCP tool invocations MUST include request IDs in logs.
Use `@Slf4j` (Lombok) for logger injection. Rationale: diagnosability in production,
measurable reliability, and observability across modular architecture.

### Versioning & Backward Compatibility
Public Maven modules and service APIs MUST use semantic versioning (MAJOR.MINOR.PATCH).
Breaking changes (MAJOR) require a documented migration plan and at least one major bump
review cycle. Non-breaking feature additions SHOULD use MINOR bumps and bug fixes a PATCH bump.
MCP schema changes must maintain compatibility or be clearly documented in release notes.
Rationale: predictable upgrades, safe rollouts, and minimal client disruption.

## Security & Compliance
Secrets MUST be stored in environment variables or a secrets manager; no secrets
in source code. Dependencies MUST be scanned for known CVEs during CI using OWASP
Dependency-Check or similar Maven plugins. Any high-severity vulnerability found in a
production dependency MUST be triaged and patched or mitigated within the timeframe
defined by team SLAs. All code MUST follow OWASP Top 10 guidelines. MCP tool definitions
MUST NOT expose sensitive parameters in logs or error messages.

## Development Workflow
Code changes MUST be introduced via pull requests (GitHub PR or equivalent). Every PR
MUST include:

- A short description of the change and its impact on public contracts (REST endpoints, MCP tools).
- Links to failing tests (before implementation) and passing tests (after).
- A checklist showing compliance with constitution gates: unit tests, contract tests,
  integration tests, observability, security scan, and a migration plan if applicable.
- Maven build success and verification that `mvn test` and `mvn verify` pass.

CI gates MUST run:
- Maven build (`mvn clean package`)
- Unit tests and coverage (`mvn test`)
- Integration tests (`mvn verify`)
- Linting and code quality checks (Checkstyle, SpotBugs)
- Dependency CVE scanning
- OpenAPI/Swagger generation validation

No PR may be merged until CI is green and two reviewers, including at least one maintainer
familiar with the impacted module, approve. At least one reviewer MUST verify MCP compliance
for changes affecting tool schemas or endpoints.

## Governance
Amendments to this constitution are made by PR against `.specify/memory/constitution.md`.
An amendment is adopted when:

1. A PR includes the proposed changes, an explanation of the rationale, and a
   migration plan for any breaking changes.
2. Maven build and all CI gates pass for the proposed change.
3. The PR receives at least two approvals, one of which MUST be a maintainer
   responsible for an impacted module (core, provider-cobol, provider-java, web, etc.).

Versioning policy for the constitution itself follows semantic versioning:

- MAJOR: backward-incompatible governance or removal/redefinition of
  non-negotiable principles.
- MINOR: addition of a principle or material expansion of guidance.
- PATCH: wording clarifications, typo fixes, or non-substantive refinements.

**Version**: 1.0.0 (Renovatio) | **Ratified**: 2025-11-26 | **Last Amended**: 2025-11-26 | **Adapted from**: TravelCBooster v1.0.0
