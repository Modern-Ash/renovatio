# Renovatio Spec-Driven Development (SDD)

This directory contains all the infrastructure and templates for **Spec-Driven Development** in Renovatio.

Renovatio is a **multi-language refactoring and migration platform** built with:
- **Language**: Java 17+
- **Framework**: Spring Boot 3.x
- **Build**: Maven
- **Architecture**: Modular (renovatio-core, renovatio-provider-*, renovatio-shared, renovatio-web, renovatio-mcp-server)
- **Interoperability**: MCP-compliant (JSON-RPC 2.0)

## What is Spec-Driven Development?

SDD is a structured approach to building features:

1. **Specification** - Write WHAT you're building (user stories, requirements, acceptance criteria)
2. **Design** - Design HOW you'll build it (data model, API contracts, database schema)
3. **Implementation** - Code it using TEST-FIRST approach (tests before code)
4. **Validation** - Verify it meets spec and follows constitutional principles

## Quick Start

### 1. Create a Feature

```bash
bash .specify/scripts/bash/create-new-feature.sh "Your feature description"

# Example:
bash .specify/scripts/bash/create-new-feature.sh "Add automated COBOL to Java migration"

# Output:
# BRANCH_NAME: 005-cobol-java-migration
# SPEC_FILE: specs/005-cobol-java-migration/spec.md
# FEATURE_NUM: 005
```

### 2. Write Specification

Edit `specs/005-your-feature/spec.md`:
- Add user stories (P1, P2, P3 priorities)
- Write acceptance scenarios (Given/When/Then)
- Define functional requirements
- Document success criteria

```markdown
# Feature Specification: Automated COBOL to Java Migration

## User Story 1 - Parse COBOL Source (Priority: P1)
**Goal**: Extract COBOL structure into intermediate representation
**Acceptance Scenarios**:
1. Given a valid COBOL program, When parsed, Then IR contains all data divisions
2. Given COBOL with copybooks, When parsed, Then copybooks are resolved

## Functional Requirements
- FR-001: System MUST parse COBOL using ProLeap/Koopa
- FR-002: System MUST generate type-safe Java classes
- FR-003: System MUST log all transformations with correlation IDs (observability)
```

### 3. Plan Implementation

Run planning workflow (via agent or manually):

```bash
# Outputs:
# - specs/005-your-feature/research.md (technical decisions)
# - specs/005-your-feature/data-model.md (entities, JPA mappings, DB schema)
# - specs/005-your-feature/contracts/ (OpenAPI/MCP schemas)
# - specs/005-your-feature/plan.md (implementation strategy)
```

### 4. Implement (Test-First)

1. **Write tests first** (in `src/test/java/`)
   - Unit tests with JUnit 5
   - Integration tests with RestAssured
   - Contract tests for OpenAPI/MCP schemas

2. **Implement code** (in `src/main/java/`)
   - Make tests pass
   - Add observability (logging, metrics, error codes)
   - Follow constitutional principles

3. **Verify compliance**
   ```bash
   mvn clean verify              # Build + all tests
   mvn dependency-check:check    # CVE scanning
   mvn checkstyle:check          # Code style
   ```

## Directory Structure

```
.specify/
├── memory/                          # SDD knowledge base
│   ├── constitution.md              # Renovatio SDD principles (Java/Spring Boot)
│   └── jira-mapping.md              # Jira ↔ SDD feature mapping example
├── templates/                       # Document templates
│   ├── spec-template.md             # Feature specification template
│   ├── plan-template.md             # Implementation plan template
│   ├── tasks-template.md            # Task breakdown template
│   └── checklist-template.md        # PR checklist template
└── scripts/bash/                    # Automation scripts
    ├── create-new-feature.sh        # Create feature branch + spec directory
    ├── setup-plan.sh                # Generate implementation plan
    ├── check-prerequisites.sh       # Verify Maven, Java, git, etc.
    ├── update-agent-context.sh      # Update agent context (internal)
    └── common.sh                    # Shared functions
```

## Core Documents

### 1. Constitution (`.specify/memory/constitution.md`)

The **Renovatio Spec-Driven Development Constitution** defines 5 core principles:

- **Module & Package First**: Reusable Maven modules for all features
- **Test-First (NON-NEGOTIABLE)**: Tests written before implementation
- **API / Contract-First**: REST/MCP schemas defined before code
- **Observability & Error Handling**: Structured logging, metrics, error codes
- **Versioning & Backward Compatibility**: Semantic versioning, migration plans

**All features MUST comply with these principles.**

### 2. Jira Mapping (`.specify/memory/jira-mapping.md`)

Shows how to map Renovatio features to Jira stories, with a complete example:
- Epic: "Java Refactoring Recipe Framework"
- User Stories: (P1) Recipe Definition, (P2) Recipe Testing, (P3) REST API, (P3) MCP Integration
- Subtasks: Design, implementation, testing tasks with Jira keys

Use this as a template for your own features.

### 3. Templates

- **`spec-template.md`**: Write feature specifications (user stories, requirements, success criteria)
- **`plan-template.md`**: Design implementation (data model, API contracts, module structure)
- **`tasks-template.md`**: Break down into tasks (one per user story, test-first)
- **`checklist-template.md`**: PR checklist ensuring constitutional compliance

## Key Concepts

### User Stories (Independent Slices)

Each feature should have multiple user stories (P1, P2, P3):
- **P1 (Critical)**: Minimum viable product (MVP), must deliver value
- **P2 (Important)**: Significant feature addition
- **P3 (Nice-to-have)**: Polish and enhancements

Each story must be **independently testable and deployable**.

Example:
- **P1**: Parser that converts COBOL to IR
- **P2**: Code generator that produces Java from IR
- **P3**: MCP tool to expose migration via AI agents

### Test-First (Constitutional Requirement)

ALL code must be tested before implementation:

```java
// 1. Write test FIRST (must fail)
@Test
public void parsesCobolProgram() {
    CobolParser parser = new CobolParser();  // Class doesn't exist yet
    CobolIR result = parser.parse(code);
    assertThat(result).isNotNull();
}

// 2. Run test - FAILS (as expected)
mvn test

// 3. Implement code to make test pass
public class CobolParser {
    public CobolIR parse(String code) {
        // ... implementation
    }
}

// 4. Run test - PASSES
mvn test
```

### Observability (Constitutional Requirement)

Every feature must include:

1. **Structured Logging** (JSON, correlation IDs)
   ```java
   @Slf4j
   public class MigrationService {
       public void migrate(String code) {
           MDC.put("correlationId", UUID.randomUUID().toString());
           log.info("Starting migration", "codeLength", code.length());
       }
   }
   ```

2. **Error Codes** (documented, machine-readable)
   ```java
   public enum MigrationError {
       PARSE_ERROR("MIG_001", "Failed to parse code"),
       INVALID_SYNTAX("MIG_002", "Invalid syntax");
   }
   ```

3. **Metrics** (Spring Boot Actuator)
   ```java
   meterRegistry.timer("migration.duration").record(duration);
   meterRegistry.counter("migration.success").increment();
   ```

### MCP Compliance (Constitutional Requirement)

If your feature exposes tools to MCP clients:

1. **Define JSON-RPC 2.0 Schema**
   ```json
   {
     "name": "migrate_cobol_to_java",
     "inputSchema": { "type": "object", "properties": {...} },
     "outputSchema": { "type": "object", "properties": {...} }
   }
   ```

2. **Implement Tool Handler** (in `renovatio-mcp-server`)

3. **Document Usage** (README, examples)

## Workflow Summary

```
1. Create Branch
   → bash .specify/scripts/bash/create-new-feature.sh "Description"
   → Creates specs/NNN-feature/ directory

2. Write Specification (1-2 days)
   → Edit specs/NNN-feature/spec.md
   → Define user stories (P1, P2, P3)
   → Review with team/agents

3. Plan Design (2-3 days)
   → Create data model (specs/NNN-feature/data-model.md)
   → Define API contracts (specs/NNN-feature/contracts/)
   → Write implementation plan (specs/NNN-feature/plan.md)

4. Implement (Test-First) (Varies)
   → For each user story:
      a. Write failing tests
      b. Implement code to pass tests
      c. Add observability (logging, metrics, error codes)
      d. Run: mvn clean verify
      e. Commit: git commit -m "feat(module): story description"

5. Create Pull Request
   → All tests passing
   → Constitutional compliance verified
   → Code review + 2 approvals
   → Merge to main
```

## Running Scripts

### Check Prerequisites

```bash
bash .specify/scripts/bash/check-prerequisites.sh

# Checks: git, Maven, Java 17+, .specify directory
```

### Create New Feature

```bash
bash .specify/scripts/bash/create-new-feature.sh \
  --short-name "cobol-migration" \
  --number 5 \
  "Add automated COBOL to Java migration"

# Creates:
# - Branch: 005-cobol-migration (if --number 5)
# - Directory: specs/005-cobol-migration/
# - File: specs/005-cobol-migration/spec.md
```

### Setup Implementation Plan (Internal)

```bash
bash .specify/scripts/bash/setup-plan.sh

# Generates planning documentation from spec
# Requires: active feature branch + spec.md
```

## Examples & Templates

### Real Example: COBOL to Java Migration

See `.specify/memory/jira-mapping.md` for a complete example:
- Epic: REN-100
- User Stories: REN-101 (Parser), REN-102 (Testing), REN-103 (API), REN-104 (MCP)
- Data model: CobolProgram, JavaGeneration entities
- API contracts: REST endpoints + MCP tool schema

### Real Example: Custom Java Recipe Framework

See `.specify/memory/jira-mapping.md` for another complete example:
- Epic: REN-100
- User Stories: REN-101 (Recipe Definition), REN-102 (Recipe Testing), etc.
- Module structure: `renovatio-provider-java-custom-recipes`
- Constitutional compliance: All 5 principles applied

## Integration with Agents & CI/CD

These scripts can be invoked by:

- **Copilot/Agents**: Agent-based planning and implementation
- **GitHub Actions**: Automated feature creation on issue comments
- **VS Code**: SDD commands via command palette
- **Manual**: Direct bash invocation

Example agent usage:

```
Agent: Create a feature specification for "New COBOL parser"
1. Agent calls: bash create-new-feature.sh "New COBOL parser"
2. Agent creates spec.md with user stories
3. Agent runs planning workflow
4. Agent generates implementation tasks
```

## Documentation

For detailed guidance on using SDD in Renovatio, see:

- **SDD Guide**: `docs/SPEC_DRIVEN_DEVELOPMENT.md` (comprehensive tutorial)
- **Constitution**: `.specify/memory/constitution.md` (principles and governance)
- **Jira Mapping**: `.specify/memory/jira-mapping.md` (example epics)
- **Git Commits**: `.github/git-commit-instructions.md` (commit message format)
- **Copilot Instructions**: `.github/copilot-instructions.md` (Renovatio stack)

## Contributing

When contributing to Renovatio using SDD:

1. **Follow the Constitution** (`.specify/memory/constitution.md`)
2. **Use templates** in `.specify/templates/`
3. **Test-first**: Write failing tests before implementation
4. **Document**: Update spec, data model, API contracts
5. **Verify**: Run `mvn clean verify` + dependency checks
6. **Commit**: Use conventional commit messages (`.github/git-commit-instructions.md`)

## Support

For questions on SDD in Renovatio:

- See `docs/SPEC_DRIVEN_DEVELOPMENT.md` (comprehensive guide)
- See `.specify/memory/constitution.md` (principles)
- See `.specify/memory/jira-mapping.md` (examples)
- Check template comments (`.specify/templates/`)

Happy building! 🚀
