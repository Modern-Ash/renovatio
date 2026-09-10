# Implementation Plan: renovatio-api

**Issue:** #130 phase 2
**Swarm:** api-layer
**Work:** renovatio-api
**Spec:** docs/specs/renovatio-api.md

## Overview

This plan breaks the renovatio-api implementation into 12 tasks, ordered by dependency. Each task produces a testable increment.

## Tasks

### Task 0: Module Skeleton + Root POM Integration
**Depends on:** None
**Criteria:** no-regression

1. Create `renovatio-api/pom.xml` with dependencies:
   - `renovatio-core`, `renovatio-shared`, `renovatio-provider-cobol`
   - `spring-boot-starter-web`, `spring-boot-starter-data-jpa`
   - `h2` (runtime), `spring-boot-starter-validation`
   - `spring-boot-starter-test` (test)
2. Create `RenovatioApiApplication.java` (Spring Boot main class)
3. Create `src/main/resources/application.yml` with H2 config
4. Add `<module>renovatio-api</module>` to root `pom.xml`
5. Add `renovatio-api` to root `<dependencyManagement>`
6. Verify: `mvn clean install -pl renovatio-api` succeeds

### Task 1: JPA Entities + Repositories
**Depends on:** Task 0
**Criteria:** persistence

1. Create `ProjectEntity.java` with `@Entity`, `@Table(name = "projects")`
   - Fields: id (UUID), name, workspacePath, branch, createdAt, updatedAt
2. Create `JobEntity.java`
   - Fields: id (UUID), projectId, operation, status, progress, paramsJson, resultJson, error, createdAt, startedAt, completedAt
3. Create `MigrationPlanSnapshotEntity.java`
   - Fields: id (UUID), projectId, planId, planContentJson, stepsJson, createdAt
4. Create `RunSnapshotEntity.java`
   - Fields: id (UUID), projectId, runId, planId, dryRun, diffJson, resultJson, startedAt, completedAt
5. Create `ActionItemEntity.java`
   - Fields: id (UUID), projectId, runId, severity, reason, requiredHumanAction, acceptanceCondition, reviewStatus, createdAt, reviewedAt
6. Create repository interfaces extending `JpaRepository<Entity, String>` for each entity
7. Create `src/test/resources/application-test.yml` with `jdbc:h2:mem:testdb`
8. Write `ProjectRepositoryTest.java` - CRUD operations
9. Verify: `mvn test -pl renovatio-api` passes

### Task 2: DTOs + Access Service
**Depends on:** Task 1
**Criteria:** role-gating

1. Create DTOs:
   - `ProjectDto.java` (id, name, workspacePath, branch, createdAt, updatedAt)
   - `JobDto.java` (id, projectId, operation, status, progress, result, error, createdAt, completedAt)
   - `PlanDto.java` (planId, planContent, steps)
   - `RunDto.java` (runId, planId, dryRun, diff, startedAt, completedAt)
   - `MetricsDto.java` (metrics map, details)
   - `ActionItemDto.java` (id, severity, reason, requiredHumanAction, acceptanceCondition, reviewStatus)
   - `JobRequestDto.java` (operation, params)
   - `ReviewStatusDto.java` (status)
2. Create `ApiAccessService.java` wrapping `ReportAccessService`:
   - `canView(AccessRole)` - delegates to `ReportAccessService.canView()`
   - `canModify(AccessRole)` - ADMIN or MANAGER
   - `canCreate(AccessRole)` - ADMIN only
3. Write `ApiAccessServiceTest.java`:
   - ADMIN can view, modify, create
   - MANAGER can view, modify, cannot create
   - VIEWER can view, cannot modify, cannot create
   - null role cannot do anything
4. Verify: `mvn test -pl renovatio-api` passes

### Task 3: PersistentPlanStore
**Depends on:** Task 1
**Criteria:** persistence

1. Create `PersistentPlanService.java` (renames to avoid collision with `MigrationPlanService`):
   - Wraps `LanguageProviderRegistry` (same as CLI's `MigrationChain`)
   - Methods: `createPlan(...)`, `applyPlan(...)`, `generateDiff(...)`
   - Persists plan snapshots and run snapshots via repositories
2. Write `PersistentPlanServiceTest.java`:
   - Test createPlan persists snapshot
   - Test applyPlan persists run snapshot
   - Test generateDiff retrieves persisted run
3. Verify: `mvn test -pl renovatio-api` passes

### Task 4: Async Config + Job Service
**Depends on:** Task 1, Task 2
**Criteria:** async-jobs

1. Create `AsyncConfig.java`:
   - `ThreadPoolTaskExecutor` bean (core=4, max=8, queue=100)
2. Create `JobService.java`:
   - `createJob(projectId, op, params)` - creates PENDING job, submits to executor, returns immediately
   - `executeJob(jobId)` - sets RUNNING, executes based on op type, sets COMPLETED/FAILED
   - Job operations: "analyze", "plan", "apply", "diff"
3. Create `SseEventCollector.java`:
   - `subscribe(jobId)` - returns `SseEmitter`
   - `send(jobId, eventType, data)` - broadcasts to subscribers
   - `complete(jobId)` - completes emitter
4. Write `JobServiceTest.java`:
   - Test createJob returns immediately with PENDING status
   - Test executeJob transitions through RUNNING to COMPLETED
   - Test failed job sets error message
5. Verify: `mvn test -pl renovatio-api` passes

### Task 5: Project Controller + Service
**Depends on:** Task 2, Task 3
**Criteria:** rest-endpoints

1. Create `ProjectService.java`:
   - `createProject(dto)` - creates project entity
   - `getProject(id)` - retrieves project
   - `listProjects()` - lists all projects
2. Create `ProjectController.java`:
   - `POST /api/projects` - creates project (requires ADMIN)
   - `GET /api/projects/{id}` - gets project (requires view access)
   - `GET /api/projects` - lists projects (requires view access)
3. Write `ProjectControllerTest.java`:
   - Test create returns 201 with project
   - Test get returns project
   - Test list returns all projects
   - Test unauthorized returns 403
4. Verify: `mvn test -pl renovatio-api` passes

### Task 6: Job Controller + SSE Endpoint
**Depends on:** Task 4, Task 5
**Criteria:** rest-endpoints, async-jobs

1. Create `JobController.java`:
   - `POST /api/projects/{id}/jobs` - creates job (requires ADMIN)
   - `GET /api/jobs/{id}` - gets job status (requires view access)
   - `GET /api/jobs/{id}/events` - SSE stream (requires view access)
2. Wire `JobService` and `SseEventCollector`
3. Write `JobControllerTest.java`:
   - Test create returns 202 with job ID
   - Test get returns job with status
   - Test SSE connection established
4. Verify: `mvn test -pl renovatio-api` passes

### Task 7: Plan + Run + Metrics Controllers
**Depends on:** Task 3, Task 5
**Criteria:** rest-endpoints

1. Create `PlanController.java`:
   - `GET /api/projects/{id}/plan` - gets plan for project (requires view access)
2. Create `RunController.java`:
   - `GET /api/projects/{id}/runs/{runId}/diff` - gets diff for run (requires view access)
3. Create `MetricsController.java`:
   - `GET /api/projects/{id}/metrics` - gets metrics (requires view access)
4. Wire `PersistentPlanService`
5. Write controller tests for each endpoint
6. Verify: `mvn test -pl renovatio-api` passes

### Task 8: Action Item Controller
**Depends on:** Task 2, Task 1
**Criteria:** rest-endpoints

1. Create `ActionItemService.java`:
   - `getActionItems(projectId)` - lists action items
   - `updateStatus(id, status)` - updates review status
2. Create `ActionItemController.java`:
   - `GET /api/projects/{id}/action-items` - lists action items (requires view access)
   - `POST /api/action-items/{id}/status` - updates status (requires modify access)
3. Write `ActionItemControllerTest.java`:
   - Test list returns action items
   - Test update status works
   - Test unauthorized returns 403
4. Verify: `mvn test -pl renovatio-api` passes

### Task 9: Integration Test - Full Job Lifecycle
**Depends on:** Task 6, Task 7, Task 8
**Criteria:** tested

1. Create `FullJobLifecycleTest.java`:
   - Test complete workflow:
     1. Create project
     2. Submit analyze job
     3. Poll job status until completed
     4. Submit plan job
     5. Get plan
     6. Submit apply job (dry-run)
     7. Get diff
     8. Get action items
     9. Update action item status
2. Verify: `mvn test -pl renovatio-api` passes

### Task 10: SSE Event Stream Test
**Depends on:** Task 6
**Criteria:** tested

1. Create `SseEventStreamTest.java`:
   - Test SSE connection receives events
   - Test job progress events are streamed
   - Test completion event is sent
2. Verify: `mvn test -pl renovatio-api` passes

### Task 11: Full Regression + Documentation
**Depends on:** All previous tasks
**Criteria:** no-regression, tested

1. Run full `mvn clean install` from root
2. Verify renovatio-core tests still pass
3. Verify renovatio-mcp-server tests still pass
4. Verify renovatio-cli tests still pass
5. Verify renovatio-api tests pass
6. Add module README.md
7. Update root README.md to mention renovatio-api
8. Verify: `java -jar renovatio-api/target/renovatio-api.jar` starts and shows H2 console at `/h2-console`

## Verification

After all tasks:
```bash
mvn clean install                    # Full build green
mvn test -pl renovatio-api           # All API tests pass
java -jar renovatio-api/target/*.jar # Starts, H2 console accessible
curl http://localhost:8080/h2-console # H2 console loads
```

## Risk Mitigation

1. **H2 file lock:** Use `jdbc:h2:file:./data/renovatio-db` with `AUTO_SERVER=TRUE` for multi-process safety
2. **Async thread safety:** `SseEventCollector` uses `ConcurrentHashMap` for thread-safe emitter management
3. **Entity mapping:** Use `MapStruct` mappers (already configured in project) for entity-DTO conversion
4. **Test isolation:** Each test class uses `jdbc:h2:mem:testdb` with `create-drop` DDL
