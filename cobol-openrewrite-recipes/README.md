# Renovatio COBOL OpenRewrite Recipes

Overview
- A collection of OpenRewrite recipes used to enrich and refactor Java code generated from the COBOL Intermediate Representation (IR).
- Complements the COBOL provider by applying conventional Java cleanups, idioms, and structure improvements after generation.

Key Features
- Recipe catalog for post-generation refactoring (naming, packaging, formatting, small transformations).
- Designed to be consumed by the Renovatio platform and by OpenRewrite tooling.
- JDK 17 compatible; tested with rewrite-java and rewrite-java-17.

Build & Test
- Build the module:
  - `mvn -DskipTests package`
- Run tests (requires JDK 17):
  - `mvn test`

Usage
- Publish the produced JAR and reference recipes from a `rewrite.yml` in other modules or via OpenRewrite CLI.
- Typical flow in Renovatio:
  1) COBOL IR to Java generation.
  2) Apply OpenRewrite recipes from this module to improve the generated sources.

Dependencies
- Depends on:
  - `renovatio-cobol-ir` (for IR compatibility).
  - OpenRewrite libraries (`rewrite-java`, `rewrite-java-17` for tests).

Notes
- Keep recipes small and composable.
- Prefer conventional Java naming/packaging patterns to match modern code standards.

