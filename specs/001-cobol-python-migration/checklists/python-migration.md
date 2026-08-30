# Checklist: Migración COBOL → Python — Unit Tests for Requirements

**Propósito:** Validar la calidad, claridad y completitud de los requisitos para la migración de código COBOL a Python (enfocado en requisitos, no en la implementación).

**Creado por:** `.specify/scripts/bash/check-prerequisites.sh` → FEATURE_DIR

## Respuestas confirmadas
> El usuario confirmó "sí" para proceder; las siguientes asunciones han sido confirmadas y quedan documentadas para trazabilidad. Si necesitas cambiar alguna, dímelo y lo actualizo.

- Confirmación C1 (alcance MVP): Limitar la primera iteración a programas secuenciales simples y copybooks locales; NO se migran automáticamente programas CICS/JCL complejos ni orquestaciones en la fase MVP. (Reduce riesgo y permite pruebas reproducibles).  
- Confirmación C2 (prioridad de traducción): Priorizar la equivalencia funcional (fidelidad 1:1) para la primera iteración; refactorización a Python idiomático en fases posteriores queda planificada.  
- Confirmación C3 (integración): Entregar inicialmente artefactos Python independientes y reproducibles; la integración con Renovatio/MCP (JSON-RPC) se realizará en una fase siguiente mediante un wrapper y contract-tests.

**Fecha de confirmación:** 2025-11-26

---

## Preguntas aclaratorias (prioritarias)
Q1: ¿El alcance de la migración debe cubrir tanto programas batch como programas CICS/JCL y sus orquestaciones, o nos limitamos inicialmente a programas secuenciales sin integración externa?  
Q2: ¿Cuál es la prioridad entre fidelidad funcional 1:1 (mantener flujo COBOL) frente a producir código Python idiomático y refactorizado?  
Q3: ¿La salida debe integrarse inmediatamente con Renovatio/MCP (exponer herramienta JSON‑RPC y metadatos), o puede entregarse como artefactos independientes para integración posterior?  

(Responde Q1..Q3 para que el checklist se pueda afinar; si tras responder quedan clases de escenarios sin resolver, pediré hasta dos preguntas adicionales.)

---

## Requirement Completeness
- [ ] CHK001 - ¿Están listadas de forma exhaustiva las unidades de código COBOL que entran en alcance (programas, copybooks, JCL, pantallas CICS)? [Completeness, Spec §Scope or Gap]
no- [x] CHK002 - ¿Están documentadas todas las dependencias externas (DB2, MQ, ficheros secuenciales, librerías CICS) que la migración debe preservar o adaptar? [Completeness, Gap]
- [x] CHK003 - ¿Se describen todas las entradas/salidas y transformaciones de datos que deben conservarse durante la migración (incluyendo formatos fijos y offsets)? [Completeness, Gap]

## Requirement Clarity
- [ ] CHK004 - ¿Está definido con precisión qué significa "funcionalmente equivalente" para esta migración (casos de ejemplo, métricas y tolerancias aceptables)? [Clarity, Gap]
- [x] CHK005 - ¿Se especifica el target de ejecución Python (versión mínima, entorno: CPython/venv/containers)? [Clarity, Gap]
- [ ] CHK006 - ¿Se cuantifican requisitos no-funcionales relevantes (por ejemplo, tiempos máximos de respuesta, throughput, memoria) para las rutas críticas? [Clarity, Gap]

## Requirement Consistency
- [ ] CHK007 - ¿Son consistentes las expectativas sobre reestructuración vs. traducción literal entre las distintas secciones del plan/req? [Consistency, Gap]
- [ ] CHK008 - ¿La estrategia de pruebas (unitarias/integración) es consistente con los criterios de aceptación funcionales definidos? [Consistency, Gap]
- [ ] CHK009 - ¿Hay coherencia entre los requisitos de persistencia transaccional y los requisitos de integridad referencial esperados por los consumidores? [Consistency, Gap]

## Acceptance Criteria Quality
- [x] CHK010 - ¿Los criterios de aceptación describen métricas/ejemplos concretos que permitan decidir si una unidad migrada es aceptable? [Acceptance Criteria, Gap]
- [ ] CHK011 - ¿Se define cómo se validará la equivalencia funcional (pruebas con entradas/salidas esperadas, tasa de tolerancia a diferencias)? [Acceptance Criteria, Gap]
- [ ] CHK012 - ¿Se especifican los criterios de aceptación para el despliegue (por ejemplo, smoke tests, rollback conditions)? [Acceptance Criteria, Gap]

## Scenario Coverage
- [ ] CHK013 - ¿Están documentados los flujos primarios (happy path) y alternativos relevantes para cada módulo migrado? [Scenario Coverage, Gap]
- [ ] CHK014 - ¿Se describen los flujos de excepción (errores de I/O, timeouts, DB deadlocks) y su tratamiento esperado post-migración? [Scenario Coverage, Gap]
- [ ] CHK015 - ¿Se incluyen escenarios de recuperación y rollback en caso de migración parcial o fallo durante la ejecución? [Scenario Coverage, Gap]

## Edge Case Coverage
- [ ] CHK016 - ¿Están las condiciones límite y valores extremos (fechas límite, campos numéricos máximos, registros corruptos) documentadas para las pruebas de migración? [Edge Case Coverage, Gap]
- [ ] CHK017 - ¿Se especifica comportamiento para datos incompletos, truncados o con encoding distinto (EBCDIC→UTF‑8)? [Edge Case Coverage, Gap]
- [ ] CHK018 - ¿Se describen expectativas para el manejo de estados intermedios y transacciones parcialmente aplicadas? [Edge Case Coverage, Gap]

## Non-Functional Requirements
- [ ] CHK019 - ¿Están especificados los requisitos de desempeño (latencia, concurrencia) para las rutas críticas migradas? [Non-Functional Requirements, Gap]
- [ ] CHK020 - ¿Se documentan requisitos de seguridad y protección de datos (en tránsito, en reposo, en logs) aplicables a los artefactos Python? [Non-Functional Requirements, Gap]
- [ ] CHK021 - ¿Se definen requisitos operacionales (monitoring, alerting, métricas) para las aplicaciones resultantes? [Non-Functional Requirements, Gap]

## Dependencies & Assumptions
- [ ] CHK022 - ¿Están listadas y verificadas las dependencias externas que deben existir para la migración (repositorios de datos, credenciales, contratos de API)? [Dependencies & Assumptions, Gap]
- [x] CHK023 - ¿Se han documentado las suposiciones sobre disponibilidad de artefactos de prueba (ejemplos COBOL, fixtures, test harness)? [Dependencies & Assumptions, Gap]
- [ ] CHK024 - ¿Existe una estrategia definida sobre cómo se validarán o actualizarán las dependencias incompatibles (por ejemplo, drivers DB2 para Python)? [Dependencies & Assumptions, Gap]

## Ambiguities & Conflicts
- [x] CHK025 - ¿Se han identificado y resuelto términos ambiguos en el spec (por ejemplo «mantener estado», «moderno», «performante»)? [Ambiguities & Conflicts, Gap]
- [ ] CHK026 - ¿Se han detectado conflictos entre requisitos (por ejemplo: consistencia transaccional vs. latencia) y están priorizados? [Ambiguities & Conflicts, Gap]
- [ ] CHK027 - ¿Se define claramente el responsable de decisiones abiertas (arquitecto, equipo de negocio, QA) y el proceso para cerrarlas? [Ambiguities & Conflicts, Gap]

---

**Notas:**
- Este checklist se centra en la calidad de los requisitos ("Unit Tests for Requirements"). No contiene pruebas de implementación ni pasos de verificación de código.  
- Muchos items referencian [Gap] porque el spec actual requiere ampliar secciones concretas; enlazar cada ítem a `Spec §...` cuando la referencia esté disponible.  

## Marcas realizadas y referencias
- CHK002: Dependencias documentadas en `specs/1-cobol-python-migration/research.md` y `specs/1-cobol-python-migration/spec.md`.  
- CHK003: Entradas/salidas y transformaciones documentadas en `specs/1-cobol-python-migration/data-model.md` y ejemplos en `examples/p1/ir_prog*.json`.  
- CHK005: Target Python y entorno documentado en `specs/1-cobol-python-migration/quickstart.md` y `requirements.txt`.  
- CHK010: Criterios de aceptación y métricas en `specs/1-cobol-python-migration/spec.md` (SC-001..SC-004).  
- CHK023: Fixtures/ejemplos añadidos bajo `specs/1-cobol-python-migration/examples/p1/`.  
- CHK025: Ambigüedades clarificadas mediante las "Confirmaciones C1..C3" (esta checklist).

**Ruta creada:** `/home/faguero/dev/renovatio/specs/001-cobol-python-migration/checklists/python-migration.md`  
**Número de ítems:** 27

**Siguiente paso:** Si confirmas las marcas incluidas, puedo crear un pequeño PR que incluya este archivo (y actualice `specs/1-cobol-python-migration/` si prefieres consolidar rutas).
