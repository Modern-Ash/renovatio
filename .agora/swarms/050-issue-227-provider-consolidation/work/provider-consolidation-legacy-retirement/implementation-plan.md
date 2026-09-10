# Implementation plan

1. Move the target-emitter registry to the shared emission boundary and retain a deprecated Core adapter for binary/source migration.
2. Move Java architecture layout policy to the architecture module so COBOL consumes target-neutral planning without depending on provider-java.
3. Replace the Java-provider OpenRewrite runner dependency with a local recipe-runner port and declare the Java 21 parser runtime directly.
4. Remove the routing switch and legacy method name; route every target through the shared registry with the built-in Java renderer represented as the registry fallback.
5. Activate ArchUnit direction rules, replace production System streams with SLF4J, and run characterization plus reactor tests.
6. Preserve PR #247 as historical provenance and deliver these corrections in a follow-up PR.
