# research.md — Migración COBOL → Python

## Objetivo
Consolidar decisiones técnicas para la feature `1-cobol-python-migration` y resolver las entradas marcadas como `NEEDS CLARIFICATION` en el spec.

Fuente: `specs/1-cobol-python-migration/spec.md`

---

## Decisiones principales

### D1 — Cobertura de alcance (respuesta a NEEDS CLARIFICATION)
- Decision: Iterar por fases. Primera iteración (MVP) se enfoca en programas secuenciales simples y copybooks locales (User Story 1). Integraciones externas (CICS, JCL, servicios externos) se tratan como artefactos marcados para intervención manual y serán abordadas en fases posteriores (User Story 2+).
- Rationale: Permite entrega rápida de valor y pruebas reproducibles.
- Alternativas: Intentar cobertura total desde el inicio (rechazada por riesgo alto de semántica perdida).

### D2 — Parser COBOL y extracción de IR
- Decision: Reusar ProLeap/Koopa (Java) ya disponible en Renovatio para parseo y extracción de AST/IR. Normalizar salida a un IR JSON intermedio (MCP-friendly) con esquema: programs[], copybooks[], records[], procedures[], io_definitions[].
- Rationale: Evita reimplementar el parser; facilita integración con módulos Java existentes.
- Alternatives: Implementar parser nativo en Python (más lento, mayor esfuerzo).

### D3 — Estrategia de generación de código
- Decision: Generación primaria en Python usando Jinja2 templates. Mantener un path alternativo: Freemarker desde Java si la integración con el ecosistema Java debe producir artefactos sin depender de un runtime Python en la fase de CI.
- Rationale: Jinja2 es simple y ampliamente usado; facilita pruebas locales con pytest.

### D4 — Manejo de tipos numéricos COBOL (COMP-3, PACKED)
- Decision: Mapear COMP-3 a Python Decimal (decimal.Decimal) usando funciones de deserialización específicas que respeten signo y escala; cuando haya pérdida, anotar en el informe y generar tests que evidencien la diferencia.
- Rationale: Decimal preserva precisión y es estándar en Python.

### D5 — Encoding y formatos fijos (EBCDIC)
- Decision: Añadir paso de conversión EBCDIC→UTF-8 como preprocesamiento usando utilidades ya existentes (iconv) o librerías Java/Python (`codecs` CP037). Los fixtures de pruebas deben incluir ejemplos tras la conversión.
- Rationale: Evita errores de parsing y asegura comparaciones de texto fiables.

### D6 — Integraciones externas (DB2, MQ, CICS, JCL)
- Decision: Detectar llamadas/contratos a subsistemas y generar adaptadores stub en Python que deleguen a servicios externos o marquen puntos manuales. No se implementará una conversión automática de transacciones DB2 en la fase MVP.
- Rationale: Minimiza riesgo y mantiene trazabilidad de puntos manuales.

### D7 — Interfaz de invocación y MCP
- Decision: La herramienta expondrá un endpoint CLI y un wrapper MCP (JSON-RPC 2.0) con esquema mínimo: input { artifacts: [paths], options: {...} } → output { artifacts: [paths], report: path }.
- Rationale: Mantener compatibilidad con Renovatio y permitir invocaciones automatizadas.

---

## Resolución de las NEEDS_CLARIFICATION del spec
- Integraciones externas y JCL: Tratar como marcado/manual en MVP (ver D1, D6).
- COMP-3 y packed decimals: Mapear a Decimal con tests (ver D4).
- Requisitos de performance no especificados: Asumir límites razonables para MVP y proponer mediciones; registrar como _non-functional TBD_.

---

## Riesgos identificados en research
- Pérdida de semántica en constructos avanzados (REDEFINES, SORT, índices) — Mitigación: detección temprana y tests manuales para módulos críticos.
- Encoding/formatos fijos — Mitigación: paso de conversión y tests de roundtrip.

---

## Recomendaciones operativas
1. Definir inmediatamente el conjunto de programas P1 (3–5 ejemplos representativos) para la primera iteración.  
2. Preparar fixtures EBCDIC originales y versiones convertidas a UTF-8.  
3. Estandarizar el IR JSON (documentar schema en `contracts/`).  
4. Implementar un generador Jinja2 con tests unitarios (pytest) para templates.

---

## Artefactos derivados (referencias)
- contracts/mcp-migration-schema.json (esquema minimal para invocación)  
- data-model.md (entidades extraídas)  



