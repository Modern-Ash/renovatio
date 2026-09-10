# Specification: renovatio-api

**Issue:** #130 phase 2
**Swarm:** api-layer
**Work:** renovatio-api
**Status:** Draft

## 1. Overview

`renovatio-api` is a new Spring Boot web module that provides a REST API oriented to UI consumption (not JSON-RPC). It adds async job execution with SSE progress streaming, H2 embedded persistence for project/job/plan/run/action-item state, and wraps the existing `MigrationPlanService` in a `PersistentPlanStore` so state survives restarts.

## 2. Module Structure

```
renovatio-api/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/org/shark/renovatio/api/
    │   │   ├── RenovatioApiApplication.java
    │   │   ├── config/
    │   │   │   ├── AsyncConfig.java
    │   │   │   └── PersistenceConfig.java
    │   │   ├── entity/
    │   │   │   ├── ProjectEntity.java
    │   │   │   ├── JobEntity.java
    │   │   │   ├── MigrationPlanSnapshotEntity.java
    │   │   │   ├── RunSnapshotEntity.java
    │   │   │   └── ActionItemEntity.java
    │   │   ├── repository/
    │   │   │   ├── ProjectRepository.java
    │   │   │   ├── JobRepository.java
    │   │   │   ├── MigrationPlanSnapshotRepository.java
    │   │   │   ├── RunSnapshotRepository.java
    │   │   │   └── ActionItemRepository.java
    │   │   ├── service/
    │   │   │   ├── ProjectService.java
    │   │   │   ├── JobService.java
    │   │   │   └── PersistentPlanStore.java
    │   │   ├── controller/
    │   │   │   ├── ProjectController.java
    │   │   │   ├── JobController.java
    │   │   │   ├── PlanController.java
    │   │   │   ├── RunController.java
    │   │   │   ├── MetricsController.java
    │   │   │   └── ActionItemController.java
    │   │   └── dto/
    │   │       ├── ProjectDto.java
    │   │       ├── JobDto.java
    │   │       ├── PlanDto.java
    │   │       ├── RunDto.java
    │   │       ├── MetricsDto.java
    │   │       └── ActionItemDto.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/org/shark/renovatio/api/
            ├── controller/
            │   ├── ProjectControllerTest.java
            │   ├── JobControllerTest.java
            │   └── ActionItemControllerTest.java
            ├── service/
            │   ├── JobServiceTest.java
            │   └── PersistentPlanStoreTest.java
            └── integration/
                └── FullJobLifecycleTest.java
```

## 3. Dependencies

### 3.1 Maven Dependencies

```xml
<dependencies>
    <!-- Core Renovatio -->
    <dependency>
        <groupId>org.shark.renovatio</groupId>
        <artifactId>renovatio-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.shark.renovatio</groupId>
        <artifactId>renovatio-shared</artifactId>
    </dependency>
    <dependency>
        <groupId>org.shark.renovatio</groupId>
        <artifactId>renovatio-provider-cobol</artifactId>
    </dependency>

    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- H2 Database (embedded) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Spring Boot Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 3.2 Parent POM Changes

Add to root `pom.xml` `<modules>`:
```xml
<module>renovatio-api</module>
```

Add to root `pom.xml` `<dependencyManagement>`:
```xml
<dependency>
    <groupId>org.shark.renovatio</groupId>
    <artifactId>renovatio-api</artifactId>
    <version>${project.version}</version>
</dependency>
```

## 4. REST Endpoints

### 4.1 Project Endpoints

| Method | Path | Description | Request | Response |
|--------|------|-------------|---------|----------|
| POST | `/api/projects` | Create a new project | `ProjectDto` (name, workspacePath, branch) | `ProjectDto` (201) |
| GET | `/api/projects/{id}` | Get project by ID | - | `ProjectDto` |
| GET | `/api/projects` | List all projects | - | `List<ProjectDto>` |

### 4.2 Job Endpoints

| Method | Path | Description | Request | Response |
|--------|------|-------------|---------|----------|
| POST | `/api/projects/{id}/jobs` | Create a new job | `JobRequestDto` (op, params) | `JobDto` (202) |
| GET | `/api/jobs/{id}` | Get job status/progress/result | - | `JobDto` |
| GET | `/api/jobs/{id}/events` | SSE stream of job progress | - | `text/event-stream` |

### 4.3 Plan/Run/Metrics Endpoints

| Method | Path | Description | Request | Response |
|--------|------|-------------|---------|----------|
| GET | `/api/projects/{id}/plan` | Get migration plan | - | `PlanDto` |
| GET | `/api/projects/{id}/runs/{runId}/diff` | Get diff for a run | - | `DiffDto` |
| GET | `/api/projects/{id}/metrics` | Get project metrics | - | `MetricsDto` |
| GET | `/api/projects/{id}/action-items` | Get action items | - | `List<ActionItemDto>` |

### 4.4 Action Item Endpoints

| Method | Path | Description | Request | Response |
|--------|------|-------------|---------|----------|
| POST | `/api/action-items/{id}/status` | Update review status | `ReviewStatusDto` (status) | `ActionItemDto` |

## 5. Data Model

### 5.1 JPA Entities

#### ProjectEntity
```java
@Entity
@Table(name = "projects")
public class ProjectEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String name;
    private String workspacePath;
    private String branch;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### JobEntity
```java
@Entity
@Table(name = "jobs")
public class JobEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String projectId;
    private String operation; // analyze, plan, apply, diff
    private String status; // PENDING, RUNNING, COMPLETED, FAILED
    private Double progress; // 0.0 - 1.0
    private String paramsJson;
    private String resultJson;
    private String error;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
```

#### MigrationPlanSnapshotEntity
```java
@Entity
@Table(name = "plan_snapshots")
public class MigrationPlanSnapshotEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String projectId;
    private String planId;
    private String planContentJson;
    private String stepsJson;
    private LocalDateTime createdAt;
}
```

#### RunSnapshotEntity
```java
@Entity
@Table(name = "run_snapshots")
public class RunSnapshotEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String projectId;
    private String runId;
    private String planId;
    private Boolean dryRun;
    private String diffJson;
    private String resultJson;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
```

#### ActionItemEntity
```java
@Entity
@Table(name = "action_items")
public class ActionItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String projectId;
    private String runId;
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW
    private String reason;
    private String requiredHumanAction;
    private String acceptanceCondition;
    private String reviewStatus; // PENDING, ACCEPTED, REJECTED
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
```

## 6. Services

### 6.1 PersistentPlanStore

Wraps `MigrationPlanService` to persist plan/run state in H2:

```java
@Service
public class PersistentPlanStore {
    private final MigrationPlanService planService;
    private final MigrationPlanSnapshotRepository planRepo;
    private final RunSnapshotRepository runRepo;

    public PlanResult createPlan(NqlQuery query, Scope scope, Workspace workspace) {
        PlanResult result = planService.createMigrationPlan(query, scope, workspace);
        // Save snapshot
        MigrationPlanSnapshotEntity entity = new MigrationPlanSnapshotEntity();
        entity.setPlanId(result.getPlanId());
        entity.setPlanContentJson(result.getPlanContent());
        planRepo.save(entity);
        return result;
    }

    public ApplyResult applyPlan(String planId, boolean dryRun, Workspace workspace) {
        ApplyResult result = planService.applyMigrationPlan(planId, dryRun, workspace);
        // Save run snapshot
        RunSnapshotEntity entity = new RunSnapshotEntity();
        entity.setRunId(result.getRunId());
        entity.setPlanId(planId);
        entity.setDryRun(dryRun);
        entity.setDiffJson(result.getDiff());
        runRepo.save(entity);
        return result;
    }
}
```

### 6.2 JobService

Manages async job execution:

```java
@Service
public class JobService {
    private final ThreadPoolTaskExecutor executor;
    private final JobRepository jobRepo;
    private final SseEventCollector eventCollector;

    public JobDto createJob(String projectId, String op, Map<String, Object> params) {
        JobEntity entity = new JobEntity();
        entity.setProjectId(projectId);
        entity.setOperation(op);
        entity.setStatus("PENDING");
        entity.setParamsJson(objectMapper.writeValueAsString(params));
        jobRepo.save(entity);

        executor.execute(() -> executeJob(entity.getId()));
        return toDto(entity);
    }

    private void executeJob(String jobId) {
        JobEntity entity = jobRepo.findById(jobId).orElseThrow();
        entity.setStatus("RUNNING");
        entity.setStartedAt(LocalDateTime.now());
        jobRepo.save(entity);

        try {
            // Execute based on operation type
            Object result = switch (entity.getOperation()) {
                case "analyze" -> executeAnalyze(entity);
                case "plan" -> executePlan(entity);
                case "apply" -> executeApply(entity);
                case "diff" -> executeDiff(entity);
                default -> throw new IllegalArgumentException("Unknown operation: " + entity.getOperation());
            };

            entity.setStatus("COMPLETED");
            entity.setResultJson(objectMapper.writeValueAsString(result));
            entity.setProgress(1.0);
        } catch (Exception e) {
            entity.setStatus("FAILED");
            entity.setError(e.getMessage());
        } finally {
            entity.setCompletedAt(LocalDateTime.now());
            jobRepo.save(entity);
            eventCollector.complete(jobId);
        }
    }
}
```

### 6.3 SseEventCollector

Manages SSE connections and event broadcasting:

```java
@Component
public class SseEventCollector {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String jobId) {
        SseEmitter emitter = new SseEmitter(0L); // No timeout
        emitters.put(jobId, emitter);
        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onTimeout(() -> emitters.remove(jobId));
        emitter.onError(e -> emitters.remove(jobId));
        return emitter;
    }

    public void send(String jobId, String eventType, Object data) {
        SseEmitter emitter = emitters.get(jobId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(objectMapper.writeValueAsString(data)));
            } catch (IOException e) {
                emitters.remove(jobId);
            }
        }
    }

    public void complete(String jobId) {
        SseEmitter emitter = emitters.remove(jobId);
        if (emitter != null) {
            emitter.complete();
        }
    }
}
```

## 7. Role-Based Access Control

Reuses existing `AccessRole` and `ReportAccessService`:

```java
@Component
public class ApiAccessService {
    private final ReportAccessService reportAccessService;

    public boolean canView(AccessRole role) {
        return reportAccessService.canView(role);
    }

    public boolean canModify(AccessRole role) {
        return role == AccessRole.ADMIN || role == AccessRole.MANAGER;
    }

    public boolean canCreate(AccessRole role) {
        return role == AccessRole.ADMIN;
    }
}
```

All endpoints check `X-Role` header:
- `GET` endpoints: require `canView()`
- `POST` endpoints (create): require `canCreate()`
- `POST` endpoints (status update): require `canModify()`

## 8. Configuration

### 8.1 application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:file:./data/renovatio-db
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
  h2:
    console:
      enabled: true
      path: /h2-console

renovatio:
  api:
    async:
      core-pool-size: 4
      max-pool-size: 8
      queue-capacity: 100
```

## 9. Acceptance Criteria

| ID | Criterion | Description |
|----|-----------|-------------|
| rest-endpoints | REST API | POST/GET /api/projects, POST /api/projects/{id}/jobs, GET /api/jobs/{id}, GET /api/jobs/{id}/events (SSE), GET /api/projects/{id}/plan\|runs/{runId}/diff\|metrics\|action-items, POST /api/action-items/{id}/status |
| async-jobs | Async Jobs | POST /api/projects/{id}/jobs returns jobId immediately; GET /api/jobs/{id} shows status/progress/result; SSE endpoint streams job progress events; ThreadPoolTaskExecutor processes jobs async |
| persistence | Persistence | H2 embedded database with JPA entities for Project, Job, MigrationPlanSnapshot, RunSnapshot, ActionItem; PersistentPlanStore wraps MigrationPlanService enabling state survives restart |
| role-gating | Role Gating | Reuses AccessRole/ReportAccessService; X-Role header controls access to endpoints; ADMIN/MANAGER can view/modify, VIEWER read-only |
| no-regression | No Regression | mvn clean install green; MCP server unchanged; renovatio-cli unchanged; existing ReportController tests pass |
| tested | Tested | Unit tests for controllers, services, repositories; integration tests for full job lifecycle; SSE event stream test |

## 10. Out of Scope

- Spring Security (uses header-based auth like existing ReportController)
- Authentication/authorization beyond role headers
- Database migrations (uses Hibernate auto-DDL)
- Pagination (can be added later)
- OpenAPI/Swagger documentation (can be added later)
