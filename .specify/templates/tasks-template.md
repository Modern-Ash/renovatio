---

description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Per the Spec-Driven Development Constitution, tests are MANDATORY for
features that affect runtime behavior or public contracts. Test tasks MUST be
written first and must fail before implementation (test-first/TDD workflow).
Include unit, contract, and integration tests as applicable.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/`, `tests/` at repository root
- **Web app**: `backend/src/`, `frontend/src/`
- **Mobile**: `api/src/`, `ios/src/` or `android/src/`
- Paths shown below assume single project - adjust based on plan.md structure

<!-- 
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.
  
  The /speckit.tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md (with their priorities P1, P2, P3...)
  - Feature requirements from plan.md
  - Entities from data-model.md
  - Endpoints from contracts/
  
  Tasks MUST be organized by user story so each story can be:
  - Implemented independently
  - Tested independently
  - Delivered as an MVP increment
  
  DO NOT keep these sample tasks in the generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Create project structure per implementation plan
- [ ] T002 Initialize [language] project with [framework] dependencies
- [ ] T003 [P] Configure linting and formatting tools

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure for TRAV-1 Epic (Registro Automático de Respuestas de Proveedores)

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

Infrastructure based on TravelCBooster monorepo structure:

- [ ] T004 Setup email metadata database schema in apps/services (PostgreSQL/DynamoDB tables)
- [ ] T005 [P] Configure LLM provider credentials and rate limiting (Bedrock, OpenAI, Ollama)
- [ ] T006 [P] Setup shared logging infrastructure with correlation IDs across packages
- [ ] T007 Create base configuration management for email processing service in apps/services/app/settings.py
- [ ] T008 Configure error handling middleware for email processing operations  
- [ ] T009 Setup S3 bucket or local storage for email attachments and audit logs

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Recepción y parsing de correos (Priority: P1) 🎯 MVP

**Jira Story**: TRAV-2 - Detectar correos entrantes, extraer contenido y adjuntos  
**Jira Link**: https://faguero.atlassian.net/browse/TRAV-2  
**Goal**: Establecer la infraestructura básica para recibir correos de proveedores y extraer su contenido de forma estructurada

**Independent Test**: Puede recibir un correo de prueba y extraer correctamente asunto, cuerpo, adjuntos y generar ID de correlación con reservas

### Tests for User Story 1 (MANDATORY per Constitution) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T010 [P] [US1] Contract test for EmailMessage schema in packages/email-processor/tests/contract/test_email_message.py (Jira: TRAV-6)
- [ ] T011 [P] [US1] Integration test for IMAP email detection in packages/email-processor/tests/integration/test_email_connector.py (Jira: TRAV-5)
- [ ] T012 [P] [US1] Integration test for email-booking correlation in tests/integration/test_correlation.py (Jira: TRAV-8)

### Implementation for User Story 1

- [ ] T013 [P] [US1] Create EmailMessage model in packages/email-processor/src/models/email_message.py (Jira: TRAV-6)
- [ ] T014 [P] [US1] Create BookingCorrelation model in packages/email-processor/src/models/correlation.py (Jira: TRAV-8)
- [ ] T015 [US1] Implement IMAP/SMTP connector in packages/email-processor/src/connectors/email_connector.py (Jira: TRAV-5)
- [ ] T016 [US1] Implement MIME parser in packages/email-processor/src/parsers/mime_parser.py (depends on T013) (Jira: TRAV-6)
- [ ] T017 [US1] Implement metadata storage service in packages/email-processor/src/storage/metadata_storage.py (Jira: TRAV-7)
- [ ] T018 [US1] Implement correlation engine in packages/email-processor/src/correlation/booking_correlator.py (depends on T014, T017) (Jira: TRAV-8)
- [ ] T019 [US1] Add structured logging for email processing events with correlation IDs
- [ ] T020 [US1] Add error handling for malformed emails and storage failures

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Análisis automático de respuesta (Priority: P2)

**Jira Story**: TRAV-3 - Usar LLM para determinar si el correo confirma, rechaza o genera duda sobre la reserva  
**Jira Link**: https://faguero.atlassian.net/browse/TRAV-3  
**Goal**: Añadir capacidad de inteligencia artificial para clasificar automáticamente las respuestas de proveedores

**Independent Test**: Puede tomar un EmailMessage procesado y devolver una clasificación estructurada (confirmado/rechazado/duda) con nivel de confianza

### Tests for User Story 2 (MANDATORY per Constitution) ⚠️

- [ ] T021 [P] [US2] Contract test for ResponseAnalysis schema in packages/ai-classifier/tests/contract/test_response_analysis.py (Jira: TRAV-11)
- [ ] T022 [P] [US2] Integration test for LLM provider calls in packages/ai-classifier/tests/integration/test_llm_provider.py (Jira: TRAV-9)
- [ ] T023 [P] [US2] Integration test for classification workflow in tests/integration/test_email_classification.py (Jira: TRAV-10)

### Implementation for User Story 2

- [ ] T024 [P] [US2] Create ResponseAnalysis model in packages/ai-classifier/src/models/response_analysis.py (Jira: TRAV-11)
- [ ] T025 [P] [US2] Create LLMProvider interface in packages/ai-classifier/src/providers/llm_provider.py (Jira: TRAV-9)
- [ ] T026 [P] [US2] Implement Bedrock provider in packages/ai-classifier/src/providers/bedrock_provider.py (Jira: TRAV-9)
- [ ] T027 [P] [US2] Implement OpenAI provider in packages/ai-classifier/src/providers/openai_provider.py (Jira: TRAV-9)
- [ ] T028 [US2] Create base prompt templates in packages/ai-classifier/src/prompts/classification_prompts.py (depends on T024) (Jira: TRAV-10)
- [ ] T029 [US2] Implement JSON schema extractor in packages/ai-classifier/src/extractors/json_extractor.py (depends on T024, T025) (Jira: TRAV-11)
- [ ] T030 [US2] Implement feedback loop mechanism in packages/ai-classifier/src/feedback/training_loop.py (Jira: TRAV-12)
- [ ] T031 [US2] Integrate with EmailMessage from User Story 1 (consume from email-processor package)
- [ ] T032 [US2] Add metrics tracking for LLM calls, response times, and confidence scores
- [ ] T033 [US2] Add error handling for LLM timeouts and malformed responses

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Registro y gestión del resultado (Priority: P3)

**Jira Story**: TRAV-4 - Actualizar Travel Compositor, mover correos procesados y dejar los dudosos para revisión  
**Jira Link**: https://faguero.atlassian.net/browse/TRAV-4  
**Goal**: Cerrar el loop completo actualizando el sistema Travel Compositor con los resultados de la clasificación y organizando los correos procesados

**Independent Test**: Puede tomar una ResponseAnalysis y actualizar correctamente el estado en Travel Compositor, además de mover el correo a la carpeta apropiada

### Tests for User Story 3 (MANDATORY per Constitution) ⚠️

- [ ] T034 [P] [US3] Contract test for TravelCompositorAPI in packages/travel-compositor-client/tests/contract/test_compositor_api.py (Jira: TRAV-13)
- [ ] T035 [P] [US3] Integration test for booking updates in packages/travel-compositor-client/tests/integration/test_booking_update.py (Jira: TRAV-15)
- [ ] T036 [P] [US3] Integration test for email management in tests/integration/test_email_management.py (Jira: TRAV-16)

### Implementation for User Story 3

- [ ] T037 [P] [US3] Create BookingUpdate model in packages/travel-compositor-client/src/models/booking_update.py (Jira: TRAV-15)
- [ ] T038 [P] [US3] Create TravelCompositorConnector in packages/travel-compositor-client/src/connectors/compositor_connector.py (Jira: TRAV-13)
- [ ] T039 [P] [US3] Implement difference validator in packages/travel-compositor-client/src/validators/booking_validator.py (Jira: TRAV-14)
- [ ] T040 [US3] Implement booking updater service in packages/travel-compositor-client/src/updaters/booking_updater.py (depends on T037, T038) (Jira: TRAV-15)
- [ ] T041 [US3] Implement email processor for moving emails in packages/travel-compositor-client/src/processors/email_processor.py (Jira: TRAV-16)
- [ ] T042 [US3] Integrate with ResponseAnalysis from User Story 2 (consume from ai-classifier package)
- [ ] T043 [US3] Generate audit logs and metrics for all Travel Compositor interactions (Jira: TRAV-17)
- [ ] T044 [US3] Add error handling for Travel Compositor API failures and email movement errors
- [ ] T045 [US3] Implement rollback mechanism for failed booking updates

**Checkpoint**: All user stories should now be independently functional

---

[Add more user story phases as needed, following the same pattern]

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] TXXX [P] Documentation updates in docs/
- [ ] TXXX Code cleanup and refactoring
- [ ] TXXX Performance optimization across all stories
- [ ] TXXX [P] Additional unit tests (if requested) in tests/unit/
- [ ] TXXX Security hardening
- [ ] TXXX Run quickstart.md validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 - Email Processing (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories. Creates email-processor package.
- **User Story 2 - AI Classification (P2)**: Can start after Foundational (Phase 2) - Consumes EmailMessage from US1 but should be independently testable. Creates ai-classifier package.
- **User Story 3 - Travel Compositor Integration (P3)**: Can start after Foundational (Phase 2) - Consumes ResponseAnalysis from US2 but should be independently testable. Creates travel-compositor-client package.

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (if tests requested):
Task: "Contract test for [endpoint] in tests/contract/test_[name].py"
Task: "Integration test for [user journey] in tests/integration/test_[name].py"

# Launch all models for User Story 1 together:
Task: "Create [Entity1] model in src/models/[entity1].py"
Task: "Create [Entity2] model in src/models/[entity2].py"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
