# Implementation plan

1. Add Spring Boot DTOs and a Workbench adapter controller that exposes logical project asset
   descriptors, not raw workspace paths.
2. Add a filesystem/service boundary that enumerates COBOL, copybooks, JCL, models, runs, evidence
   and generated targets beneath approved project roots.
3. Enforce immutable legacy extensions and an explicit, default-off development-write property for
   generated Java, Python and Node assets.
4. Add a Theia browser service that reads `RENOVATIO_BACKEND_URL`, requests the adapter and maps
   response states into the existing shell's loading, empty, permission-denied and error states.
5. Replace the deterministic fixture in the explorer with the adapter service while preserving the
   dashboard link and command/keybinding behavior.
6. Add controller, service and Workbench contract tests, including denied writes, traversal attempts,
   legacy write rejection and development-write opt-in.
7. Record build, integration and manual browser evidence; defer production identity/login enforcement
   to `workbench-identity-login`.
