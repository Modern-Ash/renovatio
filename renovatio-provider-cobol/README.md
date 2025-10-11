# Renovatio COBOL Provider

Overview
- Language provider for COBOL: parsing, analysis, metrics, planning, and migration to Java.
- Exposes MCP tools and extended capabilities for copybook and DB2 migration.

Capabilities (MCP Tools)
- `cobol.analyze`: Parse and analyze COBOL sources.
- `cobol.metrics`: Compute high-level metrics (files, lines, copybooks).
- `cobol.plan`: Create a migration plan.
- `cobol.apply`: Apply a previously created plan.
- `cobol.diff`: Generate diffs for the last run.
- `cobol.migrate_copybook`: Generate Java artifacts from a copybook (templates).
- `cobol.migrate_db2`: Generate JPA code from embedded EXEC SQL.

Key Features
- COBOL parsing and file discovery utilities.
- Template-driven code generation (Freemarker).
- DB2-to-JPA code migration helpers.
- Indexing and search with Apache Lucene for fast symbol/code lookup.

Build & Test
- Build the module:
  - `mvn -DskipTests package`
- Run tests:
  - `mvn test`

Configuration (examples)
- COBOL provider toggle:
  - `renovatio.providers.cobol.enabled=true`
- Parser dialect:
  - `renovatio.cobol.parser.dialect=IBM`
- CICS integration (mock vs real):
  - `renovatio.cics.mock=true`
  - `renovatio.cics.url=http://localhost:10080`

Dependencies
- Depends on: `renovatio-shared`, `renovatio-cobol-ir`, `cobol-openrewrite-recipes`, `renovatio-provider-java`.
- Uses MapStruct, Freemarker, Lucene, JGit, and OpenRewrite.

Notes
- Keep code and tool schemas MCP-compliant.
- Prefer configuration via `application.yml`.

