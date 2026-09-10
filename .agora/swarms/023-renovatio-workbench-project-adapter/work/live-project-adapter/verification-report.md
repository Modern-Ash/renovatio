# Verification report — live project adapter

Date: 2026-09-06

## Automated checks

- `mvn -pl renovatio-api -am -Dexec.skip=true -Dtest=WorkbenchProjectAdapterServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`: passed, 2 tests.
- `docker build --target build --tag renovatio-workbench:theia2-build .`: passed, Theia browser and node bundles reported zero errors.
- `docker run --rm --entrypoint npm renovatio-workbench:theia2-build test`: passed, 8 contract tests.

## Local integration fixture

The Spring Boot JAR was started with the two explicit development flags and
`RENOVATIO_WORKBENCH_ALLOWED_ORIGIN=http://127.0.0.1:3001`. A registered local
fixture exposed one `PAYROLL.CBL` and one generated Java target.

- `GET /api/workbench/projects` returned the registered project with no role only while the temporary development flag was enabled.
- `GET /api/projects/{id}/workbench/assets` returned COBOL as `writable:false` and Java as `writable:true`.
- `GET /assets/generated/PaymentService.java` returned its content and the configured CORS origin.
- `PUT /assets/generated/PaymentService.java` returned 204; a subsequent read confirmed the updated content.
- `PUT /assets/PAYROLL.CBL` returned 403 after the controller contract correction.

## Remaining verification

Manual browser verification is still required against a freshly launched Workbench configured with
`RENOVATIO_BACKEND_URL=http://127.0.0.1:8080` and the same configured browser origin. It must show
the live fixture, read the COBOL source, and visibly reject editing it while allowing the generated
Java target in temporary development mode.
