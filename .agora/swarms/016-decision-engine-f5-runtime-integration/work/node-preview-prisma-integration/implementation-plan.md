# Implementation plan

1. Refactor `NodePreviewService` to resolve the project and effective profile, call real workspace semantic analysis, and emit each program through the Node registry.
2. Extend Node rendering with a deterministic persistence/idiom integration seam, including shared Prisma artifacts and manual-action metadata, without changing the existing default Java path.
3. Add focused API/emitter tests for project lookup, real source paths, strategy artifacts, manual idioms, and multi-program collision behavior.
4. Run focused tests, relevant Maven compilation, characterization tests, and diff hygiene; record verification and review artifacts before the verifying transition.
