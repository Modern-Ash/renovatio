# Revisión local de equivalencia

Trabajo: `equivalence-validation-rework/cobol-target-equivalence`  
Fecha: 2026-09-05

## Resultado técnico

La implementación revisada contiene:

- `ReplayRunner`, `ReplayCoordinator` y `ReplayHarness`;
- comparación estructural con campos ignorables;
- gate con umbral configurable;
- replay por ejecutable y por fuente COBOL;
- soporte determinista de `MOVE`, `COMPUTE`, `IF`, `EVALUATE`, `PERFORM`, archivos en memoria y
  respuestas DB2 simuladas;
- persistencia e historial en API y webapp.

Los tests de `renovatio-domain-model`, `renovatio-llm`, `renovatio-cobol-ir` y la compilación de
API/UI pasan.

## Decisión

**Aprobado técnicamente para pruebas controladas.**  
**No aprobado todavía para cutover productivo.**

La aprobación productiva queda condicionada a ejecutar casos COBOL representativos contra el
runtime real, revisar divergencias y obtener aprobación del responsable de negocio.
