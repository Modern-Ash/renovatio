# Renovatio MCP Server

Overview
- Spring Boot application that exposes Renovatio tools via the Model Content Protocol (MCP).
- Bridges Renovatio providers with MCP-capable clients (e.g., editors, agents).

Key Features
- Hosts language providers (Java, COBOL) and core tools over HTTP.
- Optional OpenAPI/Swagger UI for REST endpoints.
- Health and metrics via Spring Boot Actuator.

Run
- From the module directory:
  - `mvn spring-boot:run`
- Or build and run the JAR:
  - `mvn -DskipTests package`
  - `java -jar target/renovatio-mcp-server-*.jar`

Configuration
- Use `application.yml` for server, providers, and tool settings.
- Typical flags (examples; see code for exact names):
  - `renovatio.providers.cobol.enabled=true`
  - `renovatio.providers.java.enabled=true`

Testing
- Integration tests use RestAssured.
- See `TESTING.md` for additional details.

Notes
- Ensure all exposed endpoints and tools remain MCP-compliant for interoperability.

