# Spec-Driven Development Guide for Renovatio

**Version**: 1.0.0  
**Last Updated**: 2025-11-26  
**Target Audience**: Renovatio contributors, agents, CI/CD systems

## Overview

This guide explains how to use **Spec-Driven Development (SDD)** in Renovatio - a structured approach to planning, designing, and implementing features that ensures quality, testability, and team alignment.

Renovatio is a multi-language refactoring and migration platform built with Java 17+, Spring Boot, Maven, and MCP compliance. SDD adapts the proven methodology from TravelCBooster to Renovatio's Java/Spring Boot stack and modular architecture.

## Why Spec-Driven Development?

SDD provides:

- **Clear Planning**: Before coding, specify what the feature does, who uses it, and how to test it
- **Test-First Quality**: Tests are written first, ensuring code is testable from day one
- **Shared Understanding**: Specs are reviewed by team, agents, and stakeholders
- **Independent Delivery**: Each user story can be developed, tested, and deployed separately
- **Constitutional Compliance**: Features automatically follow Renovatio's principles (modular, observable, versioned)
- **MCP Compliance**: All APIs and tools are designed for MCP interoperability from the start

## The SDD Workflow

### Phase 0: Specification (1-2 days)

**Goal**: Write what you're building, who needs it, and how to know it works.

**Deliverables**:
- `spec.md`: User stories, requirements, acceptance criteria
- `research.md`: Technical decisions and dependencies

**Templates**:
- Use `.specify/templates/spec-template.md` for the specification
- Include user stories (P1, P2, P3) as independent slices
- Define acceptance scenarios (Given/When/Then format)
- Document MCP compliance needs (if exposing tools)

**Example**: Writing a spec for "COBOL to Java Migration Tool"

```markdown
# Feature Specification: COBOL to Java Migration Tool

## User Story 1 - Parse COBOL Source Files (Priority: P1)
**Goal**: Extract COBOL structure and generate intermediate representation
**Independent Test**: Can parse sample COBOL file and produce valid IR
**Acceptance Scenarios**:
1. Given a valid COBOL program, When parsed, Then IR contains all data divisions
2. Given COBOL with copybooks, When parsed, Then copybooks are resolved

## User Story 2 - Generate Java Code from IR (Priority: P2)
**Goal**: Transform COBOL IR into Java code
**Independent Test**: Can generate valid Java from IR

## Requirements
- FR-001: System MUST parse COBOL using ProLeap/Koopa
- FR-002: System MUST generate type-safe Java classes
- FR-003: System MUST log all transformations with correlation IDs

## MCP Compliance
- Expose migration tool as JSON-RPC 2.0 endpoint
- Define input schema (COBOL code) and output schema (Java code)
```

### Phase 1: Design (2-3 days)

**Goal**: Design the solution (data model, API contracts, database schema).

**Deliverables**:
- `data-model.md`: Entities, relationships, database schema
- `contracts/`: OpenAPI specs for REST endpoints, MCP tool schemas
- `plan.md`: Implementation strategy, module structure

**Templates**:
- Use `.specify/templates/plan-template.md` for the implementation plan
- Define data entities with JPA annotations
- Design OpenAPI/Swagger endpoints or MCP tool schemas
- Create Flyway database migrations if needed
- Check constitutional compliance gates

**Example**: Data model for COBOL migration

```markdown
# Data Model: COBOL to Java Migration Tool

## Entities

### CobolProgram (JPA @Entity)
- `id` (Long @Id)
- `name` (String)
- `sourceCode` (String, @Lob)
- `status` (Enum: RECEIVED, PARSING, PARSED, GENERATING, GENERATED, ERROR)
- `createdAt` (LocalDateTime)
- `correlationId` (String, for tracing)

### JavaGeneration
- `id` (Long @Id)
- `cobolProgramId` (Long @ManyToOne)
- `generatedCode` (String, @Lob)
- `errors` (List<String>)

## Database Schema
```sql
CREATE TABLE cobol_programs (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  source_code LONGTEXT,
  status VARCHAR(50),
  created_at TIMESTAMP,
  correlation_id VARCHAR(36)
);
```

## OpenAPI Endpoints
- `POST /api/v1/migrations/cobol-to-java` - Submit COBOL file for migration
- `GET /api/v1/migrations/{id}` - Get migration status
- `GET /api/v1/migrations/{id}/result` - Get generated Java code

## MCP Tool Schema
- Tool: `migrate_cobol_to_java`
- Input: { cobolCode: string }
- Output: { javaCode: string, errors: [string] }
```

### Phase 2: Implementation (Parallel)

**Goal**: Code the feature, test-first, user story by user story.

**Workflow**:
1. **Write Tests First** (must fail before implementation)
   - Unit tests: `src/test/java/com/example/*Test.java`
   - Integration tests: `src/test/java/com/example/*IntegrationTest.java`
   - Contract tests: Validate OpenAPI/MCP schemas
   
2. **Implement Code** (make tests pass)
   - Create model classes (JPA @Entity)
   - Implement business logic (Service classes)
   - Create REST endpoints (Controller classes)
   - Expose MCP tools (if applicable)
   
3. **Add Observability**
   - Use `@Slf4j` (Lombok) for logging
   - Use MDC (Mapped Diagnostic Context) for correlation IDs
   - Add metrics to Spring Boot Actuator
   - Document error codes
   
4. **Verify Compliance**
   - Run `mvn test` (unit tests)
   - Run `mvn verify` (integration tests + Failsafe)
   - Run `mvn dependency-check:check` (CVE scanning)
   - Run `mvn checkstyle:check` (code style)

**Templates**:
- Use `.specify/templates/tasks-template.md` for task breakdown
- Organize tasks by user story (US1, US2, US3)
- Mark test tasks first (must fail initially)
- Include observability and security tasks

### Phase 3: Validation & Deployment

**Goal**: Ensure feature meets spec and is ready for production.

**Checklist**:
- [ ] All user stories are independently testable
- [ ] Tests pass: `mvn clean verify`
- [ ] Code coverage > 80% (JaCoCo)
- [ ] No CVE vulnerabilities (OWASP Dependency-Check)
- [ ] OpenAPI/MCP schemas are documented
- [ ] Error codes are documented in README
- [ ] Structured logging is in place
- [ ] Backward compatibility verified (if updating existing APIs)

## Renovatio-Specific Considerations

### Module Structure

Renovatio is organized into Maven modules. When implementing a feature, decide which module(s) it belongs to:

- **`renovatio-core`**: Core refactoring logic, shared by all providers
- **`renovatio-provider-java`**: Java-specific providers and recipes
- **`renovatio-provider-cobol`**: COBOL-specific providers and parsers
- **`renovatio-shared`**: Shared DTOs, models, utilities
- **`renovatio-web`**: REST endpoints and controllers
- **`renovatio-mcp-server`**: MCP tool definitions and handlers
- **`renovatio-*`**: New specialized modules (e.g., `renovatio-provider-xyz`)

**Example Placement**:
- New Java refactoring recipe → `renovatio-provider-java`
- New COBOL migration tool → `renovatio-provider-cobol`
- Shared migration library → `renovatio-shared`
- REST API for migrations → `renovatio-web`
- MCP exposure of migrations → `renovatio-mcp-server`

### Test Organization

```
src/test/java/org/renovatio/
├── unit/
│   └── *Test.java              # JUnit 5 unit tests (fast, isolated)
├── integration/
│   └── *IntegrationTest.java   # RestAssured + @SpringBootTest (uses DB, slower)
└── contract/
    └── *ContractTest.java      # Validate OpenAPI/MCP schemas
```

### Observability Requirements

Every feature MUST include:

1. **Structured Logging**
   ```java
   @Slf4j
   public class MigrationService {
       public void migrate(String cobolCode) {
           String correlationId = UUID.randomUUID().toString();
           MDC.put("correlationId", correlationId);
           log.info("Starting COBOL migration", "cobolLength", cobolCode.length());
       }
   }
   ```

2. **Error Codes**
   ```java
   public enum MigrationError {
       PARSE_ERROR("COBOL_PARSE_001", "Failed to parse COBOL file"),
       INVALID_SYNTAX("COBOL_SYNTAX_001", "Invalid COBOL syntax");
   }
   ```

3. **Metrics**
   ```java
   // Spring Boot Actuator automatically exposes these as /actuator/metrics
   meterRegistry.timer("migration.duration").record(duration);
   meterRegistry.counter("migration.success").increment();
   ```

### MCP Compliance

If exposing tools to MCP clients:

1. **Define JSON-RPC 2.0 Schema**
   ```json
   {
     "name": "migrate_cobol_to_java",
     "description": "Convert COBOL program to Java",
     "inputSchema": {
       "type": "object",
       "properties": {
         "cobolCode": { "type": "string" }
       },
       "required": ["cobolCode"]
     },
     "outputSchema": {
       "type": "object",
       "properties": {
         "javaCode": { "type": "string" },
         "errors": { "type": "array", "items": { "type": "string" } }
       }
     }
   }
   ```

2. **Implement Tool Handler**
   ```java
   @Component
   public class CobolToJavaTool implements McpTool {
       // Implement handle() method for JSON-RPC 2.0 invocation
   }
   ```

3. **Register with MCP Server**
   ```java
   // In renovatio-mcp-server, register tool for discovery
   mcpToolRegistry.register(new CobolToJavaTool());
   ```

## Example: Complete SDD Workflow for a Real Feature

### 1. Create Feature Branch

```bash
bash .specify/scripts/bash/create-new-feature.sh \
  --short-name cobol-java-migration \
  "Implement automated COBOL to Java migration tool"

# Output:
# BRANCH_NAME: 005-cobol-java-migration
# SPEC_FILE: specs/005-cobol-java-migration/spec.md
# FEATURE_NUM: 005
```

### 2. Write Specification

```bash
# Edit spec.md with user stories
# - US1 (P1): Parse COBOL source files
# - US2 (P2): Generate Java code from IR
# - US3 (P3): Expose via MCP tool

# Commit specification
git add specs/005-cobol-java-migration/spec.md
git commit -m "spec(provider-cobol): COBOL to Java migration - initial specification"
```

### 3. Plan Implementation

```bash
# Run planning command (via agent or manually)
# Outputs: research.md, data-model.md, contracts/, plan.md

# Commit design artifacts
git add specs/005-cobol-java-migration/
git commit -m "spec(provider-cobol): COBOL to Java migration - design and planning"
```

### 4. Implement User Story 1 (Test-First)

```bash
# Create module structure
mkdir -p renovatio-provider-cobol-migration/{src/main/java,src/test/java}

# Write test first (fails initially)
# File: src/test/java/org/renovatio/provider/cobol/CobolParserTest.java
@Test
public void parsesValidCobolProgram() {
    CobolParser parser = new CobolParser();
    CobolIR result = parser.parse(cobolCode);
    assertThat(result).isNotNull();
    assertThat(result.getDataDivisions()).hasSize(2);
}

# Run test - should FAIL
mvn test -Dtest=CobolParserTest

# Implement parser
# File: src/main/java/org/renovatio/provider/cobol/CobolParser.java
@Component
public class CobolParser {
    public CobolIR parse(String code) {
        // Implementation using ProLeap/Koopa
    }
}

# Run test - should PASS
mvn test -Dtest=CobolParserTest

# Commit implementation
git commit -m "feat(provider-cobol): implement COBOL parser (US1)"
```

### 5. Verify Compliance

```bash
# Build entire module
mvn clean verify

# Verify constitutional requirements:
mvn test                          # Unit tests pass
mvn verify                        # Integration tests pass
mvn dependency-check:check        # No CVEs
mvn checkstyle:check              # Code style OK
mvn jacoco:report                 # Coverage report

# Verify observability
# Check: correlation IDs in logs? ✓
# Check: error codes documented? ✓
# Check: metrics emitted? ✓
```

### 6. Create Pull Request

```bash
git push origin 005-cobol-java-migration

# PR Description
## COBOL to Java Migration Tool (US1 - Parser)

### What This Does
Implements COBOL parser for automated migration to Java. Parses COBOL source and generates intermediate representation (IR).

### Constitutional Compliance
- ✅ Tests-First: Unit tests written before implementation
- ✅ Contract-First: CobolIR schema defined in data-model.md
- ✅ Observability: Logging with correlation IDs, error codes defined
- ✅ Versioning: Module version 1.0.0-SNAPSHOT

### Testing
- Run: `mvn test` (5 unit tests pass)
- Run: `mvn verify` (2 integration tests pass)
- Coverage: 85%

### Related Issues
Closes REN-101.1, REN-101.2, REN-101.4, REN-101.6
```

## Tools & Scripts

### Create Feature Branch

```bash
.specify/scripts/bash/create-new-feature.sh [--short-name NAME] [--number N] "Description"
```

Creates branch, feature directory, and copies spec template.

### Check Prerequisites

```bash
.specify/scripts/bash/check-prerequisites.sh
```

Validates: Git, Maven, Java version, etc.

### Setup Planning

```bash
.specify/scripts/bash/setup-plan.sh
```

Prepares implementation plan from specification.

## Constitutional Gates

All features MUST pass these gates before merge:

| Gate | Check | Tool |
|------|-------|------|
| **Tests-First** | Unit + integration tests written before implementation | JUnit 5, RestAssured |
| **Contract-First** | API schemas defined before endpoints | OpenAPI, MCP schema validation |
| **Module First** | Code goes into appropriate Maven module | Code review |
| **Observability** | Logging, metrics, error codes | Grep for @Slf4j, MDC usage |
| **Versioning** | Semantic versioning in pom.xml | Maven version check |
| **Code Quality** | No CVEs, style passing, coverage > 80% | OWASP Dependency-Check, Checkstyle, JaCoCo |
| **MCP Compliance** | Tool schemas valid if exposed | MCP schema validator |

## Quick Reference

### Directory Structure

```
.specify/
├── memory/
│   ├── constitution.md                # Renovatio SDD principles
│   └── jira-mapping.md                # Feature → Jira mapping example
├── templates/
│   ├── spec-template.md               # Feature specification template
│   ├── plan-template.md               # Implementation plan template
│   ├── tasks-template.md              # Task breakdown template
│   └── checklist-template.md          # PR checklist template
└── scripts/bash/
    ├── create-new-feature.sh          # Create feature branch + spec
    ├── setup-plan.sh                  # Generate implementation plan
    ├── check-prerequisites.sh         # Verify Maven, Java, etc.
    └── common.sh                      # Shared functions

.github/
├── copilot-instructions.md            # Renovatio stack documentation
├── git-commit-instructions.md         # Commit message format
└── prompts/speckit.*.prompt.md        # Agent prompts for each SDD phase

docs/
└── SPEC_DRIVEN_DEVELOPMENT.md         # This guide
```

### Maven Commands

```bash
# Build entire project
mvn clean package

# Run unit tests only
mvn test

# Run unit + integration tests
mvn verify

# Check for CVEs
mvn dependency-check:check

# Check code style
mvn checkstyle:check

# Generate coverage report
mvn jacoco:report

# Run specific test class
mvn test -Dtest=CobolParserTest

# Skip tests during build
mvn clean package -DskipTests
```

### Git Workflow

```bash
# Create feature
bash .specify/scripts/bash/create-new-feature.sh "My feature description"

# Write spec
vim specs/005-my-feature/spec.md
git add specs/
git commit -m "spec(module): feature title - specification"

# Implement (test-first)
# ...write tests, implement code...
git commit -m "feat(module): feature implementation"

# Push and create PR
git push origin 005-my-feature
# Create PR with constitutional compliance checklist

# Merge after 2 approvals + CI passing
git merge 005-my-feature
```

## Common Patterns

### Pattern: Adding a New Language Provider

```
1. Create: renovatio-provider-[language]/
2. Implement: LanguageProvider interface
3. Test: Unit tests for parsing/analysis
4. Expose: REST endpoint in renovatio-web
5. MCP: Tool definition in renovatio-mcp-server
6. Document: README with examples and API docs
```

### Pattern: Adding a New Refactoring Tool

```
1. Design: Spec + data model
2. Module: Add to renovatio-core or renovatio-shared
3. Test: JUnit 5 unit tests (test-first)
4. Integration: Test with real code samples
5. Contract: Define OpenAPI/MCP schemas
6. Expose: REST endpoint or MCP tool
7. Observe: Add logging, metrics, error codes
```

### Pattern: Database Migration

```
1. Design: Entity model in data-model.md
2. Create: Flyway migration in src/main/resources/db/migration/
   V1_1__add_[table]_table.sql
3. Entity: JPA @Entity class
4. Test: Integration test with TestContainers
5. Verify: `mvn verify` runs migrations successfully
```

## Troubleshooting

### Tests Not Failing Initially

Ensure you're writing tests **before** implementation:

```java
// ❌ Wrong: Test after code
@Test
public void parserWorks() {
    CobolParser parser = new CobolParser(); // Already implemented!
    CobolIR result = parser.parse(code);    // Test passes immediately
}

// ✅ Right: Test before code
@Test
public void parserWorks() {
    CobolParser parser = new CobolParser(); // Not yet implemented
    CobolIR result = parser.parse(code);    // Test FAILS: class not found
    // ... write code to make test pass
}
```

### MCP Schema Validation Failing

Ensure JSON-RPC schema is valid:

```bash
# Validate schema syntax
cat src/main/resources/schemas/MyTool.json | jq .

# Check required fields
# - name: string
# - description: string
# - inputSchema: JSON Schema
# - outputSchema: JSON Schema
```

### Build Failing Due to Missing Dependency

Add to module's pom.xml:

```xml
<dependency>
    <groupId>org.openrewrite</groupId>
    <artifactId>rewrite-core</artifactId>
    <version>8.0.0</version>
</dependency>
```

Then rebuild: `mvn clean install`

## More Information

- **Constitution**: See `.specify/memory/constitution.md` for SDD principles
- **Architecture**: See `ARCHITECTURE.md` for module responsibilities
- **MCP Reference**: See `MCP-QUICK-REFERENCE.md` for tool definitions
- **Jira Mapping**: See `.specify/memory/jira-mapping.md` for feature examples
- **Git Commits**: See `.github/git-commit-instructions.md` for message format

## Getting Started

1. **Review the Constitution**: Read `.specify/memory/constitution.md` (5 min)
2. **Review Jira Mapping**: See `.specify/memory/jira-mapping.md` for example epic (10 min)
3. **Create Your Feature**: Run `bash .specify/scripts/bash/create-new-feature.sh` (2 min)
4. **Write Spec**: Use `.specify/templates/spec-template.md` (1-2 hours)
5. **Plan Design**: Use `.specify/templates/plan-template.md` (2-3 hours)
6. **Implement**: Test-first, commit frequently, verify compliance (varies)

Happy building! 🚀
