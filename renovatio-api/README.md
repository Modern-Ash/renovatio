# Renovatio API

REST API module for Renovatio, providing a web interface for migration management.

## Features

- **REST Endpoints**: Full CRUD operations for projects, jobs, plans, runs, metrics, and action items
- **Async Jobs**: Submit migration operations (analyze, plan, apply, diff) asynchronously with progress tracking
- **SSE Events**: Real-time job progress streaming via Server-Sent Events
- **Persistence**: H2 embedded database for storing projects, jobs, plans, runs, and action items
- **Role-Based Access Control**: X-Role header-based authorization (ADMIN, MANAGER, VIEWER)

## Endpoints

### Projects
- `POST /api/projects` - Create a new project
- `GET /api/projects/{id}` - Get project by ID
- `GET /api/projects` - List all projects

### Jobs
- `POST /api/projects/{id}/jobs` - Create a new job (analyze, plan, apply, diff)
- `GET /api/jobs/{id}` - Get job status
- `GET /api/jobs/{id}/events` - SSE stream for job progress

### Plans & Runs
- `GET /api/projects/{id}/plan` - Get migration plan
- `GET /api/projects/{id}/runs/{runId}` - Get run details
- `GET /api/projects/{id}/runs/{runId}/diff` - Get diff for a run

### Metrics & Action Items
- `GET /api/projects/{id}/metrics` - Get project metrics
- `GET /api/projects/{id}/action-items` - List action items
- `POST /api/action-items/{id}/status` - Update action item review status

## Running

```bash
# Start the API server
java -jar target/renovatio-api.jar

# Or with Maven
mvn spring-boot:run -pl renovatio-api
```

The API starts on port 8080 by default. H2 console is available at `/h2-console`.

## Configuration

Key properties in `application.yml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:file:./data/renovatio-db;AUTO_SERVER=TRUE
  jpa:
    hibernate:
      ddl-auto: update

renovatio:
  api:
    async:
      core-pool-size: 4
      max-pool-size: 8
      queue-capacity: 100
```

## Testing

```bash
# Run all tests
mvn test -pl renovatio-api

# Run specific test
mvn test -pl renovatio-api -Dtest=ProjectRepositoryTest
```

## Role Access

| Role | View | Modify | Create |
|------|------|--------|--------|
| ADMIN | ✓ | ✓ | ✓ |
| MANAGER | ✓ | ✓ | ✗ |
| VIEWER | ✗ | ✗ | ✗ |
