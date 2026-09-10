# Feature Specification: Migración COBOL a Python

**Feature Branch**: `1-cobol-python-migration`
**Created**: 26 de noviembre de 2025
**Status**: Draft
**Input**: User description: "implementación de las migraciones cobol a python tomando como ejemplo lo realizado de cobol a java"

## Project Impact

**Affected Module(s)**: `renovatio-provider-cobol`, `renovatio-core`, `renovatio-shared`, new `renovatio-provider-python` module
**API Contracts**: Posible adición de herramientas MCP para descubrimiento e invocación de migraciones
**MCP Compliance**: Requerido para las nuevas herramientas de migración (JSON-RPC 2.0 schemas)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Convertir programa COBOL simple a Python (Priority: P1)

**Jira Story**: [TBD] - Convertir programa COBOL secuencial a Python

Un desarrollador de migraciones quiere convertir un único programa COBOL (sin dependencias externas complejas) a un módulo Python que preserve la lógica de negocio y tenga pruebas unitarias equivalentes.

**Why this priority**: Permite validar la corrección de la conversión en casos simples y servir de base para casos más complejos.

**Independent Test**: Ejecutar pruebas de regresión: entradas/outputs del programa COBOL (o su test harness) comparadas con los resultados del módulo Python para el mismo conjunto de casos.

**Acceptance Scenarios**:
1. **Given** un programa COBOL con estructuras de E/S y aritmética simple, **When** se ejecuta la herramienta de migración, **Then** se genera un módulo Python que produce los mismos resultados para el conjunto de pruebas proporcionadas.
2. **Given** el paquete de entrada incluye copybooks referenciados localmente, **When** la herramienta analiza dependencias, **Then** los datos referenciados son incorporados o mapeados para permitir la ejecución del módulo Python.

**Testing Strategy**:
- Unit Tests: Validar transformaciones unitarias de constructos COBOL → Python (p. ej., MOVE, PERFORM, READ/WRITE)
- Integration Tests: Ejecutar el flujo completo con fixtures de datos y comparar salidas
- Contract Tests: Verificar que la herramienta exponga un MCP tool schema consistente

---

### User Story 2 - Migración asistida de programas COBOL con múltiples copybooks (Priority: P2)

**Jira Story**: [TBD] - Soporte a copybooks y referencias

Un ingeniero de migración quiere convertir programas con múltiples copybooks y estructuras de datos externas, obteniendo artefactos Python organizados y documentación de mapeo.

**Independent Test**: Validar que el módulo Python incluye estructuras de datos equivalentes y que los ejemplos de entrada producen salidas equivalentes.

**Acceptance Scenarios**:
1. **Given** un programa COBOL que importa copybooks, **When** se ejecuta la migración, **Then** la salida incluye estructuras de datos Python (clases/dataclasses o estructuras equivalentes) preservando offsets y tipos básicos.

---

### User Story 3 - Generación de pruebas y artefactos auxiliares (Priority: P3)

**Jira Story**: [TBD] - Generar tests y documentación de mapeo

El equipo desea que la herramienta genere pruebas unitarias y un resumen del mapeo COBOL→Python para facilitar revisiones de código.

**Independent Test**: Revisar que para cada artefacto migrado existe un test que cubre casos representativos y un documento de mapeo legible.

---

### Edge Cases

- **Integraciones externas y JCL** (resuelto — research.md D1/D6): en el MVP la herramienta NO convierte automáticamente transacciones CICS, sentencias EXEC SQL/DB2, colas MQ, `CALL` a subsistemas ni orquestación JCL. Detecta cada punto de contacto y genera un adaptador *stub* en Python (puerto de salida) más un action item en el informe de migración para intervención manual. La conversión asistida de estos casos se aborda en fases posteriores (User Story 2+).
- Manejo de datos binarios o formatos no estándar (packed decimal, COMP-3): la herramienta debe detectar y documentar incompatibilidades.
- Archivos de entrada con tamaños extremos: la migración debe documentar constraints de performance y límites aplicables.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La herramienta MUST aceptar como input un conjunto de artefactos COBOL (archivos fuente .cbl/.cob, copybooks, y metadatos) y producir un paquete Python ejecutable o invocable.
- **FR-002**: La herramienta MUST preservar la semántica observable del programa para un conjunto de pruebas representativas; se debe proporcionar un mecanismo de validación que compare resultados entre COBOL y Python.
- **FR-003**: La herramienta MUST generar artefactos de prueba (fixtures y tests) equivalentes al comportamiento observado en COBOL para facilitar la validación.
- **FR-004**: La herramienta MUST documentar las transformaciones aplicadas (mapas de campos, supuestos, y constructos no traducibles) en un informe legible.
- **FR-005**: La herramienta MUST exponer una herramienta MCP (JSON-RPC 2.0) o interfaz CLI para invocar migraciones de forma reproducible.
- **FR-006**: Para constructos no traducibles automáticamente (por ejemplo, llamadas a subsistemas externos), la herramienta MUST marcar el lugar con anotaciones y generar un “fallback” manual action item en el informe.
- **FR-007**: La herramienta MUST soportar la inclusión y resolución de copybooks locales y rutas relativas.
- **FR-008**: La herramienta MUST detectar tipos numéricos COBOL (incluyendo COMP-3) y documentar la estrategia de mapeo utilizada; cuando no se pueda mapear sin pérdida, el resultado debe incluir pruebas que evidencien la diferencia.

### Key Entities

- **COBOL Program**: Archivo fuente principal con lógica de negocio.
- **Copybook**: Fragmento reusado de estructura de datos referenciado por programas COBOL.
- **Migration Plan**: Archivo de metadatos que describe opciones y reglas para la conversión.
- **Python Artefact**: Código generado (módulo/package), tests y documentación de mapeo.
- **Validation Fixture**: Conjunto de entradas/outputs usados para validar equivalencia.

### Technical Dependencies

- **Primary Tools**: ProLeap/Koopa COBOL parser (o equivalente), utilidades de análisis sintáctico, y el motor de generación de código Python.
- **Testing**: Herramientas para ejecutar pruebas Python; comparadores de salidas (texto, CSV, registros).
- **MCP Compliance**: Definición de esquemas de entrada/salida para la herramienta de migración.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Para programas simples (definidos en P1), la tasa de éxito de conversión automática es >= 90% (definido como equivalencia de resultados sobre el conjunto de pruebas proporcionado).
- **SC-002**: Para el 100% de los artefactos migrados, la herramienta genera un informe con mapeo y una lista de puntos manuales pendientes.
- **SC-003**: Los artefactos generados incluyen tests que cubren al menos 80% de los casos representativos definidos en los fixtures de validación.
- **SC-004**: La herramienta es invocable de forma reproducible via MCP/CLI y produce artefactos versionados y trazables.

### Code Quality Gates

- Unit tests for any added modules exist and tienen cobertura razonable (meta: >80% en áreas nuevas)
- La herramienta documenta claramente las transformaciones y supuestos en el informe de migración

## Assumptions

- Por defecto asumimos que la migración apunta a código Python legible y mantenible, no a una transpilación 1:1 byte-for-byte.
- Si no se especifica, la herramienta tratará las integraciones externas como puntos que requieren intervención manual y los marcará en el informe.
- El proyecto incluirá un nuevo módulo `renovatio-provider-python` que contendrá transformaciones, mapping y generadores.

## Next Steps

1. ~~Resolver las preguntas [NEEDS CLARIFICATION]~~ — resueltas: integraciones externas/JCL → stub + action item manual en MVP (research.md D1/D6); COMP-3 → `decimal.Decimal` con tests de evidencia (D4); performance → límites razonables MVP, medición como _non-functional TBD_.
2. Prototipar la conversión para un programa COBOL sencillo (P1) y validar el proceso end-to-end.
3. Definir esquema MCP para invocación y resultados.

## Files Created

- `specs/1-cobol-python-migration/spec.md` (este archivo)
