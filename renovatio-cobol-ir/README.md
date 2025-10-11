# Renovatio COBOL Intermediate Representation (IR)

Overview
- Provides the intermediate model and utilities for analyzing COBOL programs.
- Acts as the bridge between COBOL parsing and downstream generation/refactoring steps.

Key Features
- Data structures to represent COBOL program elements and metadata.
- Utilities to assist language providers (e.g., COBOL-to-Java generation).
- Designed for interoperability with other Renovatio modules.

Build & Test
- Build the module:
  - `mvn -DskipTests package`
- Run tests:
  - `mvn test`

Dependencies
- Depends on `renovatio-shared` for shared domain, SPI, and utilities.
- Uses common Java utilities (`commons-lang3`) and Lombok for boilerplate.

When to Use
- Import this module when you need to:
  - Parse COBOL sources to an intermediate form.
  - Traverse/extract information for code generation or analysis.

