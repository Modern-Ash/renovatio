# Implementation plan

1. Add a Spring Boot shadow-impact endpoint that reads source explorer, domain
   model, and architecture canvas state.
2. Build a canonical report containing stage hashes, source impacts, artifact
   impacts, and diff summary.
3. Compare existing generated artifacts from `ProjectEntity.javaOutputPath`
   against package-relative architecture manifest entries.
4. Preserve traceability by linking artifacts only to matched domain evidence.
5. Add a Theia Shadow area with refresh/export affordances and request guards
   for project switches.
6. Validate through backend API tests, core-ui contract tests/build, remote
   GitHub CI, and resolved review threads.

