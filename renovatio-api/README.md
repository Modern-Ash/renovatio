# Renovatio API

REST API module for Renovatio, providing a web interface for migration management.

## Features

- **REST Endpoints**: Full CRUD operations for projects, jobs, plans, runs, metrics, and action items
- **Async Jobs**: Submit migration operations (analyze, plan, apply, diff) asynchronously with progress tracking
- **SSE Events**: Real-time job progress streaming via Server-Sent Events
- **Persistence**: SQLite database for storing projects, jobs, plans, runs, and action items
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

The API starts on port 8080 by default.

## Configuration

Key properties in `application.yml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:sqlite:./data/renovatio.db
  jpa:
    hibernate:
      ddl-auto: none

renovatio:
  workbench:
    # Local development only. Both flags must be explicitly true to allow
    # unauthenticated writes to generated Java/Python/Node targets.
    dev-no-auth-enabled: false
    dev-write-enabled: false
    allowed-origin: http://127.0.0.1:3000
  api:
    async:
      core-pool-size: 4
      max-pool-size: 8
      queue-capacity: 100
```

`allowed-origin` is deliberately a single explicit development origin. Configure it to the
actual Workbench origin (for example `http://127.0.0.1:3001`); do not use a wildcard.

## SQLite bootstrap and optional legacy migration

The API no longer requires a checked-in H2 database. On startup it provisions
the SQLite schema from `src/main/resources/db/sqlite/schema.sql` while keeping
Hibernate `ddl-auto: none`.

If you have a private legacy H2 database to migrate, keep it outside Git and
create the SQLite target once, then normalize temporal values before running
the API:

```bash
mvn -pl renovatio-api -Dexec.mainClass=org.shark.renovatio.api.migration.H2ToSqliteMigrator \
  -Dexec.args='jdbc:h2:file:./data/renovatio-db;AUTO_SERVER=TRUE data/renovatio.db' exec:java
mvn -pl renovatio-api -Dexec.mainClass=org.shark.renovatio.api.migration.H2ToSqliteMigrator \
  -Dexec.args='--normalize-sqlite-timestamps data/renovatio.db' exec:java
```

The migrator refuses to overwrite an existing SQLite target. If a local migration must be repeated,
first archive or deliberately remove only `data/renovatio.db`; never commit the H2 source file.

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
