# Renovatio Shared

Purpose
- Shared domain models (DTOs), SPI abstractions, utilities, and the NQL grammar used across Renovatio.
- Serves as the canonical, MCP-compliant contract between the core engine, language providers, and the MCP server.

Highlights
- Domain DTOs: AnalyzeResult, PlanResult, ApplyResult, DiffResult, MetricsResult, StubResult, Workspace, Scope, etc.
- Provider abstractions and Tool contract (protocol-agnostic).
- NQL grammar and common utilities.
- Lombok-based DTOs to reduce boilerplate (getters, setters, equals/hashCode, toString).

Requirements
- Java 17+
- Maven 3.9+
- Lombok (annotation processing enabled in your IDE/build)

Build & Test
- Build the module (skip tests):
  - `mvn -DskipTests package`
- Run tests:
  - `mvn test`

IDE Setup (Lombok)
- Enable annotation processing:
  - IntelliJ IDEA: Settings > Build, Execution, Deployment > Compiler > Annotation Processors > Enable
  - Eclipse: Preferences > Java > Compiler > Annotation Processing > Enable
- Install the Lombok plugin if your IDE requires it.

Backward Compatibility
- DTO field names and shapes are part of the public contract. Prefer additive changes and avoid breaking renames/removals.
- Keep schemas in sync with MCP expectations used by Renovatio clients.

Exported Packages
- `org.shark.renovatio.shared.domain`
- `org.shark.renovatio.shared.nql`
- `org.shark.renovatio.shared.spi`
- `org.shark.renovatio.shared.util`

Integration
- Consumed by Renovatio core, providers (Java/COBOL), and the MCP server module.
- Provided as part of the multi-module Maven build of this repository.

Contributing
- Follow project coding guidelines (English for identifiers/docs, Spring/Java best practices).
- Keep changes small, well-documented, and covered by tests when behavior changes.
