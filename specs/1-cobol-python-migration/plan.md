# plan.md — Implementación: Migración COBOL → Python

**Feature branch (actual):** `feature/1-cobol-python-migration`  
**Ubicación del plan (IMPL_PLAN):** `specs/1-cobol-python-migration/plan.md`  
**Generado por:** agente `copilot` siguiendo `.github/prompts/speckit.plan.prompt.md`  
**Fecha:** 2025-11-26

---

Resumen ejecutivo
-----------------
Este documento recoge el plan técnico para la feature `1-cobol-python-migration`. Está basado en el `specs/1-cobol-python-migration/spec.md`, `research.md`, `data-model.md` y el checklist/tasks (`tasks.md`). El objetivo es implementar un pipeline reproducible que extraiga un IR de programas COBOL, genere artefactos Python mediante plantillas (Jinja2), y valide equivalencia funcional mediante tests automatizados.

Nota operacional importante: al ejecutar `./.specify/scripts/bash/setup-plan.sh --json` desde la raíz se detectó que la rama actual `feature/1-cobol-python-migration` no cumple el patrón de nombre esperado por los scripts SDD (debe comenzar con `NNN-`); el script devolvió:

```
ERROR: Not on a feature branch. Current branch: feature/1-cobol-python-migration
Feature branches should be named like: 001-feature-name
```

Se procedió a generar este `plan.md` manualmente dentro de `specs/1-cobol-python-migration/` en lugar de usar la salida automática del script. Recomendación: renombrar la rama o crear la rama según convención para permitir que los scripts SDD funcionen automáticamente.

Variables derivadas del script (manual)
--------------------------------------
- REPO_ROOT: /home/faguero/dev/renovatio
- CURRENT_BRANCH: feature/1-cobol-python-migration
- FEATURE_DIR: /home/faguero/dev/renovatio/specs/feature/1-cobol-python-migration (nota: script infiere ``specs/feature/...``)
- FEATURE_SPEC: /home/faguero/dev/renovatio/specs/feature/1-cobol-python-migration/spec.md
- IMPL_PLAN: /home/faguero/dev/renovatio/specs/feature/1-cobol-python-migration/plan.md

(En este repo ya existe `specs/1-cobol-python-migration/` donde hemos trabajado; los scripts intentan mantener una ruta basada en el nombre de rama. Si prefieres mantener la ruta actual, no es necesario renombrar la rama, pero automatización SDD quedará limitada.)

Constitution Check (SDD gates)
-------------------------------
A continuación se evalúa el plan y los artefactos frente a la Constitución SDD (`.specify/memory/constitution.md`). Cada punto muestra el estado actual y acciones necesarias.

1. Module & Package First
   - Estado: Partial. Actualmente el PoC vive en `specs/1-cobol-python-migration/`. Recomendado: crear módulo `renovatio-provider-python/` si el artefacto será mantenido y versionado. (T028)

2. Test-First (NON-NEGOTIABLE)
   - Estado: Partial. Existen tests (pytest) que validan el generador pero no siempre fueron escritos antes del código. Para cumplir: convertir al menos un conjunto de tests (US1) a flujo test-first: crear tests fallidos, implementar y hacerlos pasar.

3. API / Contract-First
   - Estado: Partial. Se creó `contracts/mcp-migration-schema.json` (schema mínimo). Falta: exponer la herramienta como MCP JSON-RPC y añadir contract tests automáticos.

4. Observability & Error Handling
   - Estado: Deferred. Añadir logging estructurado y códigos de error en T027.

5. Versioning & Backward Compatibility
   - Estado: Deferred. Añadir packaging y políticas de versionado (T028).

Resultado Constitución: El plan cumple parcialmente las obligaciones de SDD para la fase de prototipo; las tareas de polishing (observability, packaging, MCP exposure y tests contractuales) están definidas y deben ejecutarse antes de etiquetar como compliant.


Technical Context
-----------------
- Stack actual del PoC:
  - Generador: Python 3.10+ (Jinja2)
  - Tests: pytest
  - IR: JSON (esquema provisional en `data-model.md`)
  - Parser: se prevé reutilizar ProLeap/Koopa (Java) para extracción de COBOL → IR.
- Integraciones externas relevantes: DB2, MQ, CICS, JCL. Estas se marcarán como puntos manuales en los informes si se detectan (T006, T016, T030).
- Encoding: EBCDIC→UTF-8 conversion required for many real artefacts (T007).
- Numeric types: COMP-3/packed decimals will map to Python Decimal; helpers necesarios (T008).

Unknowns / NEEDS CLARIFICATION
------------------------------
(derivados del spec y research)
- NC1: ¿La prioridad es fidelidad total del flujo COBOL (1:1) o código Python idiomático? (impacta mapping rules)  
- NC2: ¿La herramienta debe ejecutar/validar programas COBOL en entornos legacy para comparar salidas, o se usan sólo golden fixtures?  
- NC3: Alcance operativo: ¿se requiere performance equivalente en producción o se acepta re-architecting en Python para throughput?  

(Recomendación: resolver NC1..NC3 en PR del spec antes de ejecutar US2 y US3.)

Phase 0 — Research (acciones realizadas)
----------------------------------------
- `research.md` generado: decisiones D1..D7 (Cobertura, Parser choice, Codegen strategy, COMPACT handling, Encoding, Integrations, MCP interface).  
- Resumen: MVP → focus en programas secuenciales simples; ProLeap/Koopa (Java) para parsing; Jinja2 para generation; COMP-3→Decimal strategy; EBCDIC pre-conversion step.

Phase 1 — Design & Contracts (outputs)
--------------------------------------
Artefactos ya generados en este repo (evidencia):
- `specs/1-cobol-python-migration/research.md` (Decision log)  
- `specs/1-cobol-python-migration/data-model.md` (IR entities)  
- `specs/1-cobol-python-migration/contracts/mcp-migration-schema.json` (MCP minimal schema)  
- `specs/1-cobol-python-migration/quickstart.md`  
- `specs/1-cobol-python-migration/agent-context-copilot.md`

Phase 1 — Tasks remaining (high priority)
-----------------------------------------
- Implementar helpers EBCDIC (T007) y COMP-3 (T008).  
- Consolidar IR schema (T006) y formalizar `contracts/ir-schema.json`.  
- Harden generator: add logging, errors, validation (T014, T011).  
- Create integration harness and validation tool (T016, T017).

Phase 2 — Implementation Plan (milestones)
------------------------------------------
MVP milestone (2–4 sprints):
1. Foundation (1 sprint)
   - T001..T005 (setup), T006, T007, T008, T009, T010, T012
2. US1 implementation (1 sprint)
   - T013..T018 (extractor stub, generator hardening, templates tests, integration harness, validation CLI)
3. CI + Release (0.5 sprint)
   - Create CI pipeline to run generator and tests, pip-audit, and report results (T028/T029)
4. US2 and US3 (1–2 sprints)
   - Copybook resolution, dataclass generation, test/report generators (T019..T026)

Acceptance Criteria (tie to SDD)
--------------------------------
- SC-001: For P1 programs, automatic conversion rate >= 90% on the representative test set (track via integration tests).  
- SC-002: All migrated artefacts include a migration report listing manual action items (T016/T025).  
- SC-003: MCP invocation schema present and contract-tests pass (T030).  
- SC-004: CI pipeline runs for PRs and fails on missing tests or security issues (T029).

Risks & Mitigations
-------------------
- Risk: Loss of COBOL semantic in advanced constructs (REDEFINES, indexes).  
  Mitigation: Detect and flag such constructs in extractor (manual review), add unit tests for those modules. (T013, T016)

- Risk: Encoding & numeric precision issues (EBCDIC, COMP-3).  
  Mitigation: Implement T007/T008 and include roundtrip tests with fixtures.

- Risk: Operational incompatibilities (DB2 drivers, transaccionabilidad).  
  Mitigation: Create adapters/stubs and integration tests; escalate to infra if DB drivers are unavailable in CI.

Generated artifacts (to commit)
-------------------------------
- specs/1-cobol-python-migration/plan.md (este archivo)  
- specs/1-cobol-python-migration/research.md  
- specs/1-cobol-python-migration/data-model.md  
- specs/1-cobol-python-migration/contracts/mcp-migration-schema.json  
- specs/1-cobol-python-migration/quickstart.md  
- specs/1-cobol-python-migration/agent-context-copilot.md  
- specs/1-cobol-python-migration/tasks.md

Commands & Next Steps (how to run locally)
-------------------------------------------
1) Nombre de rama (opcional para activar scripts SDD automáticos):

```bash
# rename current branch to SDD-style (if desired)
git branch -m feature/1-cobol-python-migration 001-cobol-python-migration
git push --set-upstream origin 001-cobol-python-migration
```

2) Ejecutar script de setup-plan (will copy template into IMPL_PLAN):

```bash
./.specify/scripts/bash/setup-plan.sh --json
```

3) Si no se renombra la rama, proceder manualmente:

```bash
# create virtualenv & install deps for generator PoC
python3 -m venv .venv
source .venv/bin/activate
pip install -r specs/1-cobol-python-migration/requirements.txt
# run generator on example IR
python3 specs/1-cobol-python-migration/tools/generate.py --ir specs/1-cobol-python-migration/examples/p1/ir_prog1.json --templates specs/1-cobol-python-migration/templates --out specs/1-cobol-python-migration/generated
# run tests
PYTHONPATH=specs/1-cobol-python-migration pytest -q
```

4) Ejecutar update-agent-context (actualiza contexto del agente copilot):

```bash
# may require feature branch naming convention; see note above
./.specify/scripts/bash/update-agent-context.sh copilot
```

Decision log
------------
- Se preservó la decisión de usar ProLeap/Koopa para parsing (reduce esfuerzo).  
- Se eligió Jinja2 para generate (fäcil pruebas con pytest).  
- COMP-3 → Decimal como estrategia inicial; se requiere test coverage.  

Addendum — Clarified decisions (2026-08-30, Agora `delivery/cobol-python-migration`)
---------------------------------------------------------------------------------
Resolución de NC1..NC3 y alineación con la estrategia acordada de migración:

- **NC1 (fidelidad 1:1 vs idiomático):** dos fases. Fase 1 = transliteración fiel y
  estructurada a través del IR neutro (`renovatio-cobol-ir`) apoyada en un runtime que
  encapsula la semántica COBOL (para Java: `renovatio-cobol-runtime`, ya creado —
  `PicType`/`PicClause`/`CobolDecimal`/`CobolMove`/`EbcdicCollator`; para
  Python: paquete espejo `renovatio_python.cobol_runtime`, creado en
  `renovatio-provider-python`). Fase 2 = refactor incremental
  verificado hacia código idiomático. El objetivo final es Python legible y mantenible
  (ya asumido en spec.md §Assumptions), no transpilación byte-for-byte.
- **NC2 (validación):** golden fixtures / characterization testing. No se requiere
  ejecutar COBOL legacy; se capturan pares entrada/salida (ver `examples/p1/golden/`)
  y se comparan contra el módulo Python generado.
- **NC3 (performance):** se acepta re-arquitectura en Python; sin requisito de paridad
  de throughput en el MVP. Medición queda como _non-functional TBD_.
- **Integraciones externas / JCL:** stub + action item manual en MVP (spec.md §Edge Cases,
  research.md D1/D6).

Alcance de criterios de aceptación (Agora):
- ac-001 → US1: programa COBOL secuencial simple → módulo Python con salidas equivalentes.
- ac-002 → copybooks locales + COMP-3 (`decimal.Decimal`) con limitaciones documentadas.
- ac-003 → tests ejecutables + informe de transformación legible por artefacto.
- ac-004 → invocación reproducible vía MCP tool / CLI, artefactos versionados y trazables.

Dependencia: `cobol_runtime` (Python) reutiliza las reglas ya probadas en
`renovatio-cobol-runtime` (misma tabla de tipos, mismo comportamiento de MOVE/decimal).

Final notes
-----------
He generado este plan localmente dado que `setup-plan.sh` requirió una rama con prefijo numérico; el plan cubre Phases 0–2 y proporciona la lista priorizada de tareas (tasks.md).  

¿Deseas que automáticamente: (a) renombre la rama para ejecutar los scripts SDD y copie la plantilla del plan, (b) ejecute `update-agent-context.sh copilot`, y (c) cree el workflow CI minimal en `.github/workflows/ci.yml` que ejecute los tests del PoC? Indica qué combinación ejecutar y yo la ejecutaré (creando archivos y validando tests).
