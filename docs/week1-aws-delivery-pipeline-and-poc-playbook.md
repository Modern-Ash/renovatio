# Week 1 Playbook: AWS Delivery Pipeline and COBOL-to-Java PoC

- Owner: Founding Engineer
- Date: 2026-03-20
- Scope: MOD-2 week 1 deliverables

## 1. Week 1 outcomes

This playbook delivers:

1. A local development bootstrap for Renovatio-based modernization work.
2. A CI/CD entry pipeline that validates and packages the modernization server.
3. An AWS delivery handoff path (GitHub OIDC -> AWS -> CodePipeline trigger).
4. A COBOL-to-Java PoC flow with real COBOL examples and migration paths.
5. Architecture guidance for layered, hexagonal, and microservices targets.
6. Testing standards (unit, integration, e2e/regression) and release gates.

## 2. Local development environment baseline

### 2.1 Required tools

- Java 17+
- Maven 3.9+
- Docker 24+
- Git

### 2.2 Bootstrap command

From `renovatio/`:

```bash
./scripts/bootstrap-local-dev.sh
```

This script validates local toolchain versions and runs:

```bash
mvn -B -ntp clean verify
```

### 2.3 Local MCP server smoke run

```bash
mvn -B -ntp -pl renovatio-mcp-server -am package -DskipTests
java -jar renovatio-mcp-server/target/renovatio-mcp-server-*.jar
```

## 3. Delivery pipeline design (Week 1)

### 3.1 Pipeline stages

1. Source: GitHub push/pull request.
2. Validate: compile + tests across Renovatio modules.
3. Package: build `renovatio-mcp-server` artifact.
4. Delivery trigger: optional AWS trigger on manual dispatch.

### 3.2 Implemented workflow

Workflow file:

- `.github/workflows/modernization-poc-delivery.yml`

Key points:

- Runs `mvn clean verify` on `push`/`pull_request`.
- Produces build artifacts for `renovatio-mcp-server`.
- Supports `workflow_dispatch` with `deploy=true` to trigger an existing AWS CodePipeline.
- Uses OIDC role assumption (`aws-actions/configure-aws-credentials`) instead of static AWS keys.

### 3.3 AWS prerequisites for deployment stage

Before enabling deploy in production:

1. Create IAM role for GitHub OIDC trust and grant `codepipeline:StartPipelineExecution`.
2. Add repo secret `AWS_DEPLOY_ROLE_ARN`.
3. Create AWS CodePipeline named for your environment (`modernash-renovatio-dev` by default).
4. Run workflow dispatch with:
   - `deploy=true`
   - `aws_region` set to target region
   - `codepipeline_name` set to your pipeline

## 4. COBOL-to-Java PoC technical flow

### 4.1 Recommended modernization sequence

1. `cobol.analyze`: parse programs, dependencies, and structures.
2. `cobol.metrics`: quantify complexity and scope.
3. `cobol.plan`: produce migration plan.
4. `cobol.apply`: generate Java artifacts from plan.
5. `cobol.diff`: review generated changes before merge.

### 4.2 Real example A: data structure mapping

COBOL input:

```cobol
01  CUSTOMER-RECORD.
    05  CUSTOMER-ID    PIC 9(8).
    05  CUSTOMER-NAME  PIC X(50).
```

Migration target (layered style):

```java
@Entity
public class Customer {
    @Id
    private Long customerId;
    private String customerName;
}
```

Path:

- COBOL Data Division -> Java entity/DTO
- File/table operations -> repository
- Business paragraphs -> service methods

### 4.3 Real example B: control-break processing

COBOL pattern (`READ` loop + break detection) maps to service decomposition:

- Repository interface for record source
- Aggregation service for subtotals
- Processing orchestrator for execution flow

This follows Renovatio's documented control-break decomposition and avoids direct 1:1 procedural translation.

### 4.4 Real example C: embedded SQL / DB2 path

When `EXEC SQL` appears:

- Use `cobol.migrate_db2` for initial extraction.
- Map SQL blocks to Spring Data repositories and transactional service boundaries.
- Preserve transaction semantics before optimizing query strategy.

## 5. Architecture patterns to apply

### 5.1 Layered architecture (default for simple PoCs)

Use when:

- Limited integration complexity
- Fast delivery is the primary objective

Structure:

- API -> Service -> Repository -> Persistence

### 5.2 Hexagonal architecture (default for migration robustness)

Use when:

- Multiple input/output interfaces
- Need to swap adapters (file, DB, API) during phased migration

Structure:

- Domain core + inbound/outbound ports + adapters

### 5.3 Microservices (selectively)

Use when:

- Bounded contexts are clear
- Team can support distributed ops

Rule for PoC phase:

- Split only where operational boundaries are already explicit in the legacy domain.

## 6. Testing framework baseline

### 6.1 Unit tests

- JUnit 5
- Domain/service tests for translated business rules

### 6.2 Integration tests

- Spring Boot integration tests
- Testcontainers for DB-backed components

### 6.3 End-to-end / regression

- Golden dataset comparison (COBOL expected outputs vs Java outputs)
- Contract tests for exposed REST endpoints

### 6.4 Quality gates in pipeline

- Build fails on test failures
- Artifact generated only after full validation stage passes

## 7. AWS best practices and deployment patterns

### 7.1 Security and identity

- Prefer GitHub OIDC federation for CI/CD access to AWS.
- Avoid long-lived IAM user access keys in CI secrets.

### 7.2 Environments

- Isolate `dev`, `staging`, and `prod` accounts or at least roles.
- Use separate CodePipeline executions per environment.

### 7.3 Runtime patterns

- ECS Fargate for long-running modernization APIs.
- Lambda for small, stateless transformation hooks.
- Step Functions for orchestration when migration involves multi-step async jobs.

### 7.4 Observability

- CloudWatch logs and metrics for pipeline and runtime stages.
- Structured JSON logs from modernization services.

## 8. Week 1 acceptance checklist

- [x] Local development bootstrap script is available.
- [x] CI/CD workflow is added and runnable.
- [x] AWS trigger path is defined with OIDC + CodePipeline.
- [x] Real COBOL examples and migration mappings are documented.
- [x] Architecture pattern selection guide is documented.
- [x] Testing and quality gates are documented.

## 9. Week 2 recommended next steps

1. Provision `dev` AWS CodePipeline and validate end-to-end trigger.
2. Add infrastructure-as-code module (CDK/Terraform) for repeatable environment creation.
3. Execute PoC on one representative COBOL workload and capture baseline metrics.
4. Add performance and cost benchmarks per migration pattern.
