# tasks.md — 1-cobol-python-migration

**Feature**: `1-cobol-python-migration`
**Based on**: `specs/1-cobol-python-migration/spec.md`, `research.md`, `data-model.md`, `contracts/`

## Phase 1: Setup

- [ ] T001 Create project README and quickstart in `specs/1-cobol-python-migration/README.md` (ensure steps, requirements.txt present)  
- [ ] T002 Create `specs/1-cobol-python-migration/requirements.txt` with pinned deps (`jinja2`, `pytest`)  
- [ ] T003 [P] Initialize Python virtualenv helper script `specs/1-cobol-python-migration/scripts/setup_env.sh` (creates .venv, installs -r requirements.txt)  
- [ ] T004 [P] Add `.gitignore` for generated artifacts at `specs/1-cobol-python-migration/.gitignore` (ignore .venv, generated/)  
- [ ] T005 Create `specs/1-cobol-python-migration/tasks.md` (this file) as the executable task list

## Phase 2: Foundational (blocking prerequisites)

- [ ] T006 [US1] Define and commit IR JSON schema at `specs/1-cobol-python-migration/contracts/ir-schema.json` (fields: programs, copybooks, records, io_definitions)  
- [ ] T007 Implement EBCDIC→UTF-8 utility `specs/1-cobol-python-migration/tools/ebcdic_convert.py` with unit tests in `specs/1-cobol-python-migration/tests/test_ebcdic.py`  
- [ ] T008 Implement COMP-3 pack/unpack helper `specs/1-cobol-python-migration/tools/comp3.py` with unit tests `specs/1-cobol-python-migration/tests/test_comp3.py`  
- [ ] T009 [P] Standardize IR examples: move `examples/p1/ir_prog*.json` into `specs/1-cobol-python-migration/examples/` (already present) and add golden fixtures under `specs/1-cobol-python-migration/examples/p1/golden/`  
- [ ] T010 Create `specs/1-cobol-python-migration/mapping_rules.md` documenting COBOL→Python mapping decisions (COMP-3→Decimal, REDEFINES strategy)  
- [ ] T011 Implement generator CLI wrapper `specs/1-cobol-python-migration/tools/cli.py` that validates input against `contracts/mcp-migration-schema.json` and calls `tools/generate.py`  
- [ ] T012 [P] Create `specs/1-cobol-python-migration/contracts/mcp-migration-schema.json` (ensure it's the authoritative schema for MCP invocation)  

## Phase 3: User Stories (priority order)

### User Story 1 - Convertir programa COBOL simple a Python (P1)

Goal: Produce a Python module that preserves observable behavior for simple COBOL programs and tests validating equivalence.

Independent Test Criteria: Given IR + fixtures, generator produces Python module and `pytest` integration test passes comparing outputs to golden fixtures.

- [ ] T013 [US1] Implement COBOL-to-IR extractor runner skeleton (Java) `renovatio-provider-cobol/tools/cli-extractor.sh` or `specs/1-cobol-python-migration/tools/extractor_stub.sh` that documents expected extractor inputs/outputs  
- [ ] T014 [US1] Implement Python generator core `specs/1-cobol-python-migration/tools/generate.py` (MVP exists) — review and add logging and error handling at `specs/1-cobol-python-migration/tools/generate.py`  
- [ ] T015 [US1] Add unit tests for generator templates in `specs/1-cobol-python-migration/tests/test_templates.py` (assert rendered code compiles)  
- [ ] T016 [US1] Create integration test harness `specs/1-cobol-python-migration/tests/integration/test_end_to_end.py` that: 1) runs extractor (or loads IR), 2) runs generator, 3) executes generated module and compares to `examples/p1/golden/` outputs  
- [ ] T017 [US1] Implement validation CLI `specs/1-cobol-python-migration/tools/validate.py` that compares COBOL outputs (or golden baseline) vs Python output with tolerance modes (exact / numeric tolerance)  
- [ ] T018 [US1] Document acceptance and usage in `specs/1-cobol-python-migration/quickstart.md` (update commands to use new CLI wrappers)  

### User Story 2 - Migración asistida con múltiples copybooks (P2)

Goal: Support resolving and inlining or mapping copybooks and generating structured Python dataclasses.

Independent Test Criteria: For programs with copybooks, generated Python includes dataclass representations matching record layouts and integration tests validate offsets/fields.

- [ ] T019 [US2] Implement copybook resolution logic `specs/1-cobol-python-migration/tools/copybook_resolver.py` that locates and inlines referenced copybooks (paths resolution)  
- [ ] T020 [US2] Generate Python dataclasses for records in `specs/1-cobol-python-migration/templates/dataclass.py.j2` and ensure generation writes to `generated/models.py`  
- [ ] T021 [US2] Create unit tests `specs/1-cobol-python-migration/tests/test_copybook_resolver.py` with sample copybooks in `examples/p1/copybooks/`  
- [ ] T022 [US2] Update integration test harness to validate copybook-based programs `specs/1-cobol-python-migration/tests/integration/test_copybooks.py`  
- [ ] T023 [US2] Generate mapping report entries for copybooks in migration report `specs/1-cobol-python-migration/generated/report.md`  

### User Story 3 - Generación de pruebas y artefactos auxiliares (P3)

Goal: Automatically produce unit tests and a mapping document for reviewers.

Independent Test Criteria: For each generated module, a test skeleton exists under `generated/tests/` and mapping document `generated/mapping.md` is produced.

- [ ] T024 [US3] Implement test skeleton generator `specs/1-cobol-python-migration/tools/testgen.py` that writes pytest skeletons into `generated/tests/`  
- [ ] T025 [US3] Implement mapping document generator `specs/1-cobol-python-migration/tools/reportgen.py` that appends mapping entries to `generated/mapping.md`  
- [ ] T026 [US3] Add review checklist generator `specs/1-cobol-python-migration/tools/review_checklist.py` that emits PR checklist items for code reviewers  

## Final Phase: Polish & Cross-cutting Concerns

- [ ] T027 Add logging and observability hooks in generator and CLI: structured JSON logs and correlation IDs in `specs/1-cobol-python-migration/tools/logging.py`  
- [ ] T028 [P] Add packaging and release task: create `setup.py` or `pyproject.toml` and CI publish job `ci/publish.yml`  
- [ ] T029 Add security scanning step in CI: `pip-audit` or `safety` configured in `.github/workflows/ci.yml`  
- [ ] T030 Create MCP JSON-RPC wrapper `specs/1-cobol-python-migration/mcp_server.py` exposing `migrate(artifacts, options)` and contract-tests `specs/1-cobol-python-migration/tests/test_mcp_contract.py`  
- [ ] T031 [P] Create documentation runbook `specs/1-cobol-python-migration/docs/runbook.md` with rollback and manual-action procedures

## Dependencies & Execution Order (high level)

- Phase 1 (T001..T005) must complete before Phase 2 tasks that run in CI.  
- Foundational (T006..T012) are prerequisites for user stories; notably, IR schema (T006) and EBCDIC/COMP-3 helpers (T007/T008) enable robust generation.  
- User Story 1 (T013..T018) depends on Foundational tasks (T006..T012).  
- User Story 2 depends on T019..T023 and on US1 success.  
- User Story 3 tasks (T024..T026) can run in parallel with US2 after generator core (T014) exists.  
- Polish tasks (T027..T031) should be executed after at least US1 and US2 pass integration tests.

## Parallel execution opportunities

- T003 (env setup) and T004 (.gitignore) are parallelizable [P].  
- T006 (IR schema) and T010 (mapping_rules.md) can be worked in parallel with coordination.  
- T019 (copybook resolver) and T020 (dataclass templates) are parallelizable after IR schema exists.  
- T024..T026 (test/report generators) can run in parallel after generator core is ready [P].

## Task counts and summary

- Total tasks: 31
- Tasks per user story:  
  - US1: 6 tasks (T013..T018)  
  - US2: 5 tasks (T019..T023)  
  - US3: 3 tasks (T024..T026)  
- Foundational tasks: 7 (T006..T012)  
- Setup tasks: 5 (T001..T005)  
- Polish tasks: 5 (T027..T031)

## Independent test criteria (per user story)

- US1: generator must produce Python module; `tests/integration/test_end_to_end.py` must run and assert outputs equal golden fixtures.  
- US2: copybook resolution must create dataclasses; offsets must match expected values in `tests/test_copybook_resolver.py`.  
- US3: for each generated module a test skeleton and mapping entry must be present in `generated/tests/` and `generated/mapping.md` respectively.

## Suggested MVP scope

- MVP: Implement Phase 1 (T001..T005) + Foundational (T006..T012) + US1 (T013..T018). This provides a working pipeline for simple programs and validation harness.

## Format validation

All tasks follow the required checklist format with Task IDs (T001..), story labels for user story tasks `[US1]`, and file paths provided.

---

**Output path**: `specs/1-cobol-python-migration/tasks.md`

**Summary**: 31 tasks created; recommended next actions: implement T003 (setup script), T006 (IR schema), T007/T008 (EBCDIC & COMP-3 helpers), T014 (generator hardening), T016 (integration harness), and create CI workflow referencing these tasks.

